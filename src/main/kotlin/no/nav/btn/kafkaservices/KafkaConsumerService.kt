package no.nav.btn.kafkaservices

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(KafkaConsumerService::class.java)

abstract class KafkaConsumerService(val bootstrapServer: String = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092") {
    protected abstract val SERVICE_APP_ID: String
    protected open val HTTP_PORT: Int = 8080
    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM)
    private val kafkaConsumerMetrics = KafkaConsumerMetrics()
    private lateinit var applicationEngine: ApplicationEngine
    lateinit var job: Job

    fun start(withNaisHealthChecks: Boolean = false) {
        if (withNaisHealthChecks) {
            kafkaConsumerMetrics.bindTo(registry)
            DefaultExports.initialize()
            applicationEngine = naisHttpChecks().start(wait = false)
        }

        job = Job()
        run()
    }

    fun stop() {
        logger.info("Shutting down $SERVICE_APP_ID")
        job.cancel()
        if (::applicationEngine.isInitialized) {
            applicationEngine.stop(gracePeriod = 5, timeout = 5, timeUnit = TimeUnit.SECONDS)
        }
        shutdown()
    }

    abstract fun run()

    abstract fun shutdown()

    open fun getConsumerConfig(): Properties {
        return consumerConfig(groupId = SERVICE_APP_ID, bootstrapServerUrl = bootstrapServer)
    }

    open fun getProducerConfig(): Properties {
        return producerConfig(clientId = SERVICE_APP_ID + "_producer", bootstrapServers = bootstrapServer)
    }

    private fun naisHttpChecks(): ApplicationEngine {
        return embeddedServer(Netty, HTTP_PORT) {
            routing {
                get("/isAlive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }
                get("/isReady") {
                    call.respondText("READY", ContentType.Text.Plain)
                }
                get("/metrics") {
                    val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                    }
                }
            }
        }
    }

}