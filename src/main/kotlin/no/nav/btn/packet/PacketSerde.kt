package no.nav.btn.packet

import com.google.gson.GsonBuilder
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class PacketSerializer : Serializer<Packet> {
    val gson = GsonBuilder().create()

    override fun serialize(topic: String?, data: Packet?): ByteArray? {
        return data?.let {
            gson.toJson(data).toByteArray()
        }
    }

}

class PacketDeserializer : Deserializer<Packet> {
    val gson = GsonBuilder().create()

    override fun deserialize(topic: String?, data: ByteArray?): Packet? {
        return data?.let {
            gson.fromJson(String(data), Packet::class.java)
        }
    }

}