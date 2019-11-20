package no.nav.btn.kafkaservices

import no.nav.btn.packet.Packet
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * En Sink er et endepunkt for data, typisk et datalager, for eksempel en database. Kun for konsum,
 * sender ikke data videre.
 */
abstract class Sink(
        val consumerTopics: List<String>,
        val credential: KafkaCredential? = null
) : KafkaConsumerService() {

    private val logger = LoggerFactory.getLogger(Sink::class.java)

    override fun run() {
        val consumer = KafkaConsumer<String, Packet>(getConsumerConfig(credential = credential))
        consumer.subscribe(consumerTopics)
        while(job.isActive) {
            val records = consumer.poll(Duration.ofMillis(100))
            records.forEach {
                logger.info("$SERVICE_APP_ID har mottatt pakke ${it.key()}")
                onRecordRecieved(it)
            }
        }
    }

    abstract fun onRecordRecieved(record: ConsumerRecord<String, Packet>)

    override fun shutdown() {
        logger.info("Shutting down $SERVICE_APP_ID...")
    }
}
