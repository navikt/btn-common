package no.nav.btn.kafkaservices

import no.nav.btn.packet.PacketDeserializer
import no.nav.btn.packet.PacketSerializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.btn.Configuration")

data class KafkaCredential(val username: String, val password: String) {
        override fun toString(): String {
                return "username '$username' password '*******'"
        }
}

val defaultConsumerConfig = Properties().apply {
    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1")
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PacketDeserializer::class.java.name)
}

fun consumerConfig(
        groupId: String,
        bootstrapServerUrl: String,
        credential: KafkaCredential? = null,
        properties: Properties = defaultConsumerConfig
): Properties {
    return Properties().apply {
        putAll(properties)
        putAll(commonConfig(bootstrapServerUrl, credential))
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
}

val defaultProducerConfig = Properties().apply {
    put(ProducerConfig.ACKS_CONFIG, "1")
    put(ProducerConfig.BATCH_SIZE_CONFIG, "1")
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, PacketSerializer::class.java.name)
}

fun producerConfig(
        clientId: String,
        bootstrapServers: String,
        credential: KafkaCredential? = null,
        properties: Properties = defaultProducerConfig
): Properties {
    return Properties().apply {
        putAll(properties)
        putAll(commonConfig(bootstrapServers, credential))
        put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    }
}

fun commonConfig(bootstrapServers: String, credential: KafkaCredential? = null): Properties {
    return Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            credential?.let { creds ->
                    putAll(credentials(creds))
            }
    }
}

private fun credentials(credential: KafkaCredential): Properties {
    return Properties().apply {
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";"
        )

        val trustStoreLocation = System.getenv("NAV_TRUSTSTORE_PATH")
        trustStoreLocation?.let {
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
                logger.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (e: Exception) {
                logger.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ", e)
            }
        }
    }
}
