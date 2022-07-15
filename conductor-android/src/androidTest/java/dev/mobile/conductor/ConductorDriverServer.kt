package dev.mobile.conductor

import android.util.Log
import androidx.test.uiautomator.UiDevice
import conductor.android.models.DeviceInfoResponse
import conductor.android.models.TapRequest
import conductor.android.models.ViewHierarchyResponse
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

class ConductorDriverServer(
    private val uiDevice: UiDevice,
) {

    fun start() {
        embeddedServer(Netty, port = 7001) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    },
                    contentType = ContentType.Application.Json
                )
            }

            routing {
                deviceInfo()
                viewHierarchy()
                tap()
                health()
            }
        }.start(wait = true)
    }

    private fun Route.health() {
        get("/health") {
            Log.d("Conductor", "Health: OK")
            call.respond(HttpStatusCode.OK)
        }
    }

    private fun Route.deviceInfo() {
        get("/device/info") {
            Log.d("Conductor", "Get device info")
            call.respond(
                DeviceInfoResponse(
                    widthPixels = uiDevice.displayWidth,
                    heightPixels = uiDevice.displayHeight,
                )
            )
        }
    }

    private fun Route.viewHierarchy() {
        get("/device/hierarchy") {
            Log.d("Conductor", "Get hierarchy")

            val hierarchy = withContext(Dispatchers.IO) {
                val stream = ByteArrayOutputStream()
                uiDevice.dumpWindowHierarchy(stream)
                stream.toString(Charsets.UTF_8.name())
            }

            Log.d("Conductor", "Hierarchy obtained")

            call.respond(
                ViewHierarchyResponse(
                    hierarchy = hierarchy,
                )
            )
        }
    }

    private fun Route.tap() {
        post("/device/tap") {
            Log.d("Conductor", "Tap on element")

            val request = call.receive<TapRequest>()
            uiDevice.click(request.x, request.y)

            Log.d("Conductor", "Tap on element complete")

            call.respond(HttpStatusCode.OK)
        }
    }

}