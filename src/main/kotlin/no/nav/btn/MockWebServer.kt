package no.nav.btn

import java.lang.Exception

fun makeMockServerCall() {
    Thread.sleep(100)
    if (Math.random() * 10 > 5.0) {
        throw Exception("Fail")
    }
}
