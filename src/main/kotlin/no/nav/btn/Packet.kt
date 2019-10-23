package no.nav.btn

data class Breadcrumb(val clientId: String)

open class Packet {
    var breadcrumbs: MutableList<Breadcrumb> = mutableListOf()

}