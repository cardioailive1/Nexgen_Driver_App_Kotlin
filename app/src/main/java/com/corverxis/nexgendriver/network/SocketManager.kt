package com.corverxis.nexgendriver.network

import com.corverxis.nexgendriver.Config
import com.corverxis.nexgendriver.data.*
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketManager {
    private val gson = Gson()
    private val socket: Socket = IO.socket(Config.API_BASE)

    var onConnect: (() -> Unit)? = null
    var onDriverState: ((DriverState) -> Unit)? = null
    var onRequestNew: ((RideDTO) -> Unit)? = null
    var onTripAssigned: ((RideDTO) -> Unit)? = null
    var onTripFinalized: ((TripFinalizedResult) -> Unit)? = null
    var onOnlineRejected: ((String) -> Unit)? = null
    var onApplicationDecision: ((String, String?) -> Unit)? = null // (status, reviewNotes)
    var onAccountDeactivated: ((String) -> Unit)? = null
    var onTripRated: (() -> Unit)? = null
    var onTripTipped: ((Double) -> Unit)? = null
    var onRewardTierUp: ((String, Int) -> Unit)? = null // (tier, points)

    init {
        socket.on(Socket.EVENT_CONNECT) { onConnect?.invoke() }
        socket.on("driver:state") { args -> emitToHandler(args, DriverState::class.java, onDriverState) }
        socket.on("request:new") { args -> emitToHandler(args, RideDTO::class.java, onRequestNew) }
        socket.on("trip:assigned") { args -> emitToHandler(args, RideDTO::class.java, onTripAssigned) }
        socket.on("trip:finalized") { args -> emitToHandler(args, TripFinalizedResult::class.java, onTripFinalized) }
        socket.on("driver:onlineRejected") { args ->
            val obj = args.getOrNull(0) as? JSONObject
            val reason = obj?.optString("reason")
            if (!reason.isNullOrEmpty()) onOnlineRejected?.invoke(reason)
        }
        // Pushed the moment an admin approves/rejects — no polling needed.
        socket.on("application:decision") { args ->
            val obj = args.getOrNull(0) as? JSONObject
            val status = obj?.optString("status")
            if (!status.isNullOrEmpty()) {
                val notes = obj.optString("reviewNotes").takeIf { obj.has("reviewNotes") && !obj.isNull("reviewNotes") }
                onApplicationDecision?.invoke(status, notes)
            }
        }
        // Pushed immediately by an admin deactivating this account.
        socket.on("account:deactivated") { args ->
            val obj = args.getOrNull(0) as? JSONObject
            val reason = obj?.optString("reason") ?: "Your account has been deactivated. Contact support."
            onAccountDeactivated?.invoke(reason)
        }
        // A rider just rated this trip — Insights can refresh live.
        socket.on("trip:rated") { onTripRated?.invoke() }
        // A rider just added a tip.
        socket.on("trip:tipped") { args ->
            val obj = args.getOrNull(0) as? JSONObject
            val amount = obj?.optDouble("amount")
            if (amount != null && !amount.isNaN()) onTripTipped?.invoke(amount)
        }
        // Crossed into a new reward tier.
        socket.on("reward:tierUp") { args ->
            val obj = args.getOrNull(0) as? JSONObject
            val tier = obj?.optString("tier")
            val points = obj?.optInt("points") ?: 0
            if (!tier.isNullOrEmpty()) onRewardTierUp?.invoke(tier, points)
        }
    }

    private fun <T> emitToHandler(args: Array<Any>, clazz: Class<T>, handler: ((T) -> Unit)?) {
        val json = (args.getOrNull(0) as? JSONObject)?.toString() ?: return
        val parsed = gson.fromJson(json, clazz) ?: return
        handler?.invoke(parsed)
    }

    fun connect() = socket.connect()
    fun disconnect() = socket.disconnect()

    // ---- Emits — mirror frontend/app.js exactly ----
    fun driverJoin(token: String) {
        socket.emit("driver:join", JSONObject(mapOf("token" to token)))
    }

    fun driverOnline(lat: Double, lng: Double) {
        socket.emit("driver:online", JSONObject(mapOf("lat" to lat, "lng" to lng)))
    }

    fun driverOffline() {
        socket.emit("driver:offline", JSONObject())
    }

    fun driverLocation(lat: Double, lng: Double) {
        socket.emit("driver:location", JSONObject(mapOf("lat" to lat, "lng" to lng)))
    }

    fun requestAccept(requestId: String) {
        socket.emit("request:accept", JSONObject(mapOf("requestId" to requestId)))
    }

    fun requestDecline(requestId: String) {
        socket.emit("request:decline", JSONObject(mapOf("requestId" to requestId)))
    }

    fun tripArrived(requestId: String) {
        socket.emit("trip:arrived", JSONObject(mapOf("requestId" to requestId)))
    }

    fun tripCancel(requestId: String) {
        socket.emit("trip:cancel", JSONObject(mapOf("requestId" to requestId)))
    }

    fun tripStarted(requestId: String) {
        socket.emit("trip:started", JSONObject(mapOf("requestId" to requestId)))
    }

    fun tripComplete(requestId: String, distanceKm: Double, durationMin: Double) {
        socket.emit(
            "trip:complete",
            JSONObject(
                mapOf(
                    "requestId" to requestId,
                    "distanceKm" to distanceKm, "durationMin" to durationMin
                )
            )
        )
    }
}
