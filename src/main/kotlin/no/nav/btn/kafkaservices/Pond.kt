package no.nav.btn.kafkaservices

import no.nav.btn.packet.Packet
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

abstract class Pond(val consumerTopics: List<String>) : ConsumerService() {

    override fun run() {
        val consumer = KafkaConsumer<String, Packet>(getConsumerConfig())
        consumer.subscribe(consumerTopics)
        while(job.isActive) {
            val records = consumer.poll(Duration.ofMillis(100))
            records.forEach {
                onRecordRecieved(it)
            }
        }
    }

    abstract fun onRecordRecieved(record: ConsumerRecord<String, Packet>)
}