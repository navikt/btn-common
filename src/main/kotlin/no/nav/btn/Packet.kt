package no.nav.btn

data class Breadcrumb(val clientId: String)

class Packet (
        val breadcrumbs: List<Breadcrumb>,
        val timestamp: Long,
        val message: String
)