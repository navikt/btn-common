package no.nav.btn.packet

data class Breadcrumb(val clientId: String)

class Packet (
        val breadcrumbs: List<Breadcrumb>,
        val timestamp: Long,
        val melding: String
)