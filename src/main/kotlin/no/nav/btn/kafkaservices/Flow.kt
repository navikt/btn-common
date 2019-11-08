package no.nav.btn.kafkaservices

import no.nav.btn.TOPIC_DEAD_LETTER
import no.nav.btn.packet.Breadcrumb
import no.nav.btn.packet.Packet
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import java.time.Duration

private const val FAILED_ATTEMPTS_HEADER = "X-Failed-Attempts"
private const val MAXIMUM_FAILED_ATTEMPTS = 10

/**
 * En Flow er en kafka-konsument som også sender data videre. En Flow tar imot en pakke, gjør noe
 * (for eksempel et kall til baksystem), lager en ny pakke med ekstra data som er resultat av operasjonen
 * og sender denne pakken ut på et nytt topic. Dersom operasjonen feiler, sendes pakken ut på et retry-topic
 * der man kan forsøke igjen.
 */
abstract class Flow(
        val consumerTopics: List<String>,
        val successfullTopic: String,
        val retryTopic: String
) : KafkaConsumerService() {
    lateinit var reproducer: KafkaProducer<String, Packet>
    private val logger = LoggerFactory.getLogger(Flow::class.java)

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
                        .filter { r -> filter(r) }
                        .onEach { r -> logger.info("$SERVICE_APP_ID har mottatt pakke ${r.key()}") }
                        .map { r ->
                            val result = runCatching {
                                onPacketRecieved(r.value())
                            }
                            when {
                                result.isFailure -> {
                                    logger.error("$SERVICE_APP_ID meldte feil på melding: ${errorMessage(result.exceptionOrNull())}")
                                    makeFailedProducerRecord(r)
                                }
                                else -> ProducerRecord(successfullTopic, r.key(), addBreadcrumbs(result.getOrThrow()))
                            }
                        }
                        .forEach { sendRecord(it) }
            }
        }
    }

    abstract fun onPacketRecieved(packet: Packet): Packet

    open fun filter(record: ConsumerRecord<String, Packet>): Boolean {
        return true
    }

    private fun errorMessage(err: Throwable?): String =
            err?.message ?: "Tom feil"

    private fun makeFailedProducerRecord(record: ConsumerRecord<String, Packet>): ProducerRecord<String, Packet> {
        val failedAttempts = numberOfFailedAttempt(record)
        if (failedAttempts == MAXIMUM_FAILED_ATTEMPTS) {
            return ProducerRecord(TOPIC_DEAD_LETTER,
                    null,
                    record.key(),
                    record.value(),
                    listOf(RecordHeader(FAILED_ATTEMPTS_HEADER, "$failedAttempts".toByteArray())))
        }
        return ProducerRecord(retryTopic,
                null,
                record.key(),
                record.value(),
                listOf(RecordHeader(FAILED_ATTEMPTS_HEADER, "${failedAttempts + 1}".toByteArray())))
    }

    private fun numberOfFailedAttempt(record: ConsumerRecord<String, Packet>) =
            record.headers()?.lastHeader(FAILED_ATTEMPTS_HEADER)?.value()?.let { String(it).toInt() } ?: 0

    private fun sendRecord(producerRecord: ProducerRecord<String, Packet>) {
        logger.info("$SERVICE_APP_ID sender videre ${producerRecord.key()}")
        reproducer.send(producerRecord)
    }

    override fun shutdown() {
        if (::reproducer.isInitialized) {
            reproducer.flush()
            reproducer.close(Duration.ofSeconds(5))
        }
    }

    private fun addBreadcrumbs(packet: Packet): Packet = Packet(
            breadcrumbs = packet.breadcrumbs + Breadcrumb(SERVICE_APP_ID),
            timestamp = packet.timestamp,
            melding = packet.melding
    )
}