package no.nav.btn.kafkaservices

import no.nav.btn.packet.Packet
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

/**
 * En source er en kafkaprodusent. Sender en melding ut på et topic.
 */
class Source(
        val producerTopic: String,
        val clientId: String,
        bootstrapServer: String = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"
) {
    private val logger = LoggerFactory.getLogger(Source::class.java)
    private val producer: KafkaProducer<String, Packet> = KafkaProducer(producerConfig(clientId, bootstrapServer))

    fun producePacket(packet: Packet) {
        val uuid = UUID.randomUUID().toString()
        logger.info("$clientId sender pakke med id $uuid")
        producer.send(ProducerRecord(producerTopic, uuid, packet))
    }
}