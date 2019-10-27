package no.nav.btn.kafkaservices

import no.nav.btn.TOPIC_MULTIPLE_FAILING_SERVICE_CALLS
import no.nav.btn.packet.Breadcrumb
import no.nav.btn.packet.Packet
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val logger = LoggerFactory.getLogger(RiverWithServiceCall::class.java)
private const val FAILED_ATTEMPTS_HEADER = "X-Failed-Attempts"
private const val MAXIMUM_FAILED_ATTEMPTS = 10

abstract class RiverWithServiceCall(
        val consumerTopics: List<String>,
        val onSuccessfullCallTopic: String,
        val retryOnFailedCallTopic: String
) : ConsumerService() {
    lateinit var reproducer: KafkaProducer<String, Packet>

    private fun initializeReproducer() {
        reproducer = KafkaProducer(getProducerConfig())
    }

    override fun run() {
        if (!::reproducer.isInitialized) {
            initializeReproducer()
        }

        KafkaConsumer<String, Packet>(getConsumerConfig()).use { consumer ->
            consumer.subscribe(consumerTopics)
            while (job.isActive) {
                val records = consumer.poll(Duration.ofMillis(100))
                records.asSequence()
                        .onEach { r -> logger.info("Mottatt ${r.key()}") }
                        .filter { r -> filter(r) }
                        .map { r ->
                            val result = runCatching {
                                makeServiceCall(r.value())
                            }
                            when {
                                result.isFailure -> makeFailedProducerRecord(r)
                                else -> ProducerRecord(onSuccessfullCallTopic, UUID.randomUUID().toString(), addBreadcrumbs(result.getOrThrow()))
                            }
                        }
                        .forEach { sendRecord(it) }
            }
        }
    }

    abstract fun makeServiceCall(packet: Packet): Packet

    private fun filter(record: ConsumerRecord<String, Packet>): Boolean {
        // TODO: Legg til filter for sett før
        return true
    }

    private fun makeFailedProducerRecord(record: ConsumerRecord<String, Packet>): ProducerRecord<String, Packet> {
        logger.error("Message failure")
        val failedAttempts = numberOfFailedAttempt(record)
        if (failedAttempts == MAXIMUM_FAILED_ATTEMPTS) {
            return ProducerRecord(TOPIC_MULTIPLE_FAILING_SERVICE_CALLS,
                    null,
                    record.key(),
                    record.value(),
                    listOf(RecordHeader(FAILED_ATTEMPTS_HEADER, "$failedAttempts".toByteArray())))
        }
        return ProducerRecord(retryOnFailedCallTopic,
                null,
                record.key(),
                record.value(),
                listOf(RecordHeader(FAILED_ATTEMPTS_HEADER, "${failedAttempts + 1}".toByteArray())))
    }

    private fun numberOfFailedAttempt(record: ConsumerRecord<String, Packet>) =
            record.headers()?.lastHeader(FAILED_ATTEMPTS_HEADER)?.value()?.let { String(it).toInt() } ?: 0

    private fun sendRecord(producerRecord: ProducerRecord<String, Packet>) {
        reproducer.send(producerRecord)
    }

    override fun shutdown() {
        if (::reproducer.isInitialized) {
            reproducer.flush()
            reproducer.close(Duration.ofSeconds(5))
        }
    }

    private fun addBreadcrumbs(packet: Packet): Packet = Packet(
            breadcrumbs = packet.breadcrumbs + Breadcrumb("btn-brukermelding-oppgave"),
            timestamp = packet.timestamp,
            message = packet.message
    )
}