package com.corverxis.nexgendriver.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corverxis.nexgendriver.data.*
import com.corverxis.nexgendriver.location.LocationProvider
import com.corverxis.nexgendriver.network.ApiClient
import com.corverxis.nexgendriver.network.RouteInfo
import com.corverxis.nexgendriver.network.RoutingService
import com.corverxis.nexgendriver.network.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen { LOGIN, APPLICATION, DASHBOARD, TRIP, COMPLETE }
enum class TripPhase { TO_PICKUP, READY_START, IN_TRIP, READY_COMPLETE }

class DriverViewModel(
    private val context: Context,
    private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val locationProvider = LocationProvider(context)
    private val socket = SocketManager()

    // ---- Identity / session ----
    private val _screen = MutableStateFlow(AppScreen.LOGIN)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    var driverId: String? = null
    var driverName: String = ""

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // ---- Location ----
    val currentCoordinate: StateFlow<Coordinate?> = locationProvider.coordinate

    // ---- Online status ----
    private val _online = MutableStateFlow(false)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val _onlineSeconds = MutableStateFlow(0)
    val onlineSeconds: StateFlow<Int> = _onlineSeconds.asStateFlow()

    // ---- Dashboard stats ----
    private val _earnings = MutableStateFlow(0.0)
    val earnings: StateFlow<Double> = _earnings.asStateFlow()

    private val _trips = MutableStateFlow(0)
    val trips: StateFlow<Int> = _trips.asStateFlow()

    private val _history = MutableStateFlow<List<TripHistoryItem>>(emptyList())
    val history: StateFlow<List<TripHistoryItem>> = _history.asStateFlow()

    // ---- Incoming request ----
    private val _incomingRequest = MutableStateFlow<RideDTO?>(null)
    val incomingRequest: StateFlow<RideDTO?> = _incomingRequest.asStateFlow()

    private val _requestSecondsRemaining = MutableStateFlow(12)
    val requestSecondsRemaining: StateFlow<Int> = _requestSecondsRemaining.asStateFlow()

    // ---- Active trip ----
    private val _currentRide = MutableStateFlow<RideDTO?>(null)
    val currentRide: StateFlow<RideDTO?> = _currentRide.asStateFlow()

    private val _phase = MutableStateFlow(TripPhase.TO_PICKUP)
    val phase: StateFlow<TripPhase> = _phase.asStateFlow()

    private val _route = MutableStateFlow<RouteInfo?>(null)
    val route: StateFlow<RouteInfo?> = _route.asStateFlow()

    private val _etaMinutes = MutableStateFlow(0)
    val etaMinutes: StateFlow<Int> = _etaMinutes.asStateFlow()

    private val _meterFare = MutableStateFlow(0.0)
    val meterFare: StateFlow<Double> = _meterFare.asStateFlow()

    private var tripDistanceKm = 0.0
    private var tripStartMillis = 0L
    private var meterJob: kotlinx.coroutines.Job? = null
    private var requestTimerJob: kotlinx.coroutines.Job? = null
    private var onlineTimerJob: kotlinx.coroutines.Job? = null

    // ---- Trip result ----
    private val _lastResult = MutableStateFlow<TripFinalizedResult?>(null)
    val lastResult: StateFlow<TripFinalizedResult?> = _lastResult.asStateFlow()

    // ---- Payouts ----
    private val _payoutsEnabled = MutableStateFlow(false)
    val payoutsEnabled: StateFlow<Boolean> = _payoutsEnabled.asStateFlow()

    private val _payoutsConnected = MutableStateFlow(false)
    val payoutsConnected: StateFlow<Boolean> = _payoutsConnected.asStateFlow()

    // ---- Driver application ----
    private val _application = MutableStateFlow(DriverApplication.EMPTY)
    val application: StateFlow<DriverApplication> = _application.asStateFlow()

    private val _applicationError = MutableStateFlow<String?>(null)
    val applicationError: StateFlow<String?> = _applicationError.asStateFlow()

    private val _uploadingDoc = MutableStateFlow<DocType?>(null)
    val uploadingDoc: StateFlow<DocType?> = _uploadingDoc.asStateFlow()

    // ---- Insights ----
    private val _insights = MutableStateFlow<DriverInsights?>(null)
    val insights: StateFlow<DriverInsights?> = _insights.asStateFlow()

    // ---- Fare rates (for consistent estimates) ----
    private val _fareRates = MutableStateFlow<FareRates?>(null)
    val fareRates: StateFlow<FareRates?> = _fareRates.asStateFlow()

    private val _tierUpAlert = MutableStateFlow<String?>(null)
    val tierUpAlert: StateFlow<String?> = _tierUpAlert.asStateFlow()
    fun dismissTierUpAlert() { _tierUpAlert.value = null }

    fun refreshInsights() {
        val id = driverId ?: return
        viewModelScope.launch {
            try {
                _insights.value = ApiClient.api.getInsights(id)
            } catch (_: Exception) { /* leave previous value */ }
        }
    }

    init {
        locationProvider.onUpdate = { coord ->
            if (_online.value) {
                socket.driverLocation(coord.lat, coord.lng)
            }
        }
        wireSocket()

        val savedToken = prefs.getString("nexgen_token", null)
        val savedId = prefs.getString("nexgen_driverId", null)
        if (savedToken != null && savedId != null) {
            ApiClient.token = savedToken
            driverId = savedId
            restoreSession()
        }
    }

    // ---- Authentication ----
    // Email + password so a driver can log back in from any device and
    // keep their approved status — identity is no longer just a random ID
    // stored in this one app's SharedPreferences.
    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = ApiClient.api.register(mapOf("name" to name, "email" to email, "password" to password))
                applySession(result.token, result.driverId, result.driver.name)
                enterApp()
            } catch (e: retrofit2.HttpException) {
                _loginError.value = extractError(e) ?: "Could not create your account."
            } catch (e: Exception) {
                _loginError.value = "Could not create your account. Check your connection and try again."
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = ApiClient.api.login(mapOf("email" to email, "password" to password))
                applySession(result.token, result.driverId, result.driver.name)
                enterApp()
            } catch (e: retrofit2.HttpException) {
                _loginError.value = extractError(e) ?: "Could not sign in."
            } catch (e: Exception) {
                _loginError.value = "Could not sign in. Check your connection and try again."
            }
        }
    }

    /** Validates the saved token against the server before trusting it — an
     * expired or revoked token sends the driver back to login instead of
     * every subsequent request failing silently. */
    private fun restoreSession() {
        viewModelScope.launch {
            try {
                val driver = ApiClient.api.me()
                driverName = driver.name
                prefs.edit().putString("nexgen_driverName", driver.name).apply()
                enterApp()
            } catch (e: Exception) {
                logOut()
            }
        }
    }

    private fun applySession(token: String, id: String, name: String) {
        ApiClient.token = token
        driverId = id
        driverName = name
        prefs.edit()
            .putString("nexgen_token", token)
            .putString("nexgen_driverId", id)
            .putString("nexgen_driverName", name)
            .apply()
    }

    fun logOut() {
        ApiClient.token = null
        driverId = null
        driverName = ""
        prefs.edit().clear().apply()
        _screen.value = AppScreen.LOGIN
    }

    fun changePassword(current: String, new: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = ApiClient.api.changePassword(mapOf("currentPassword" to current, "newPassword" to new))
                onResult(if (result.ok == true) null else (result.error ?: "Could not update password."))
            } catch (e: retrofit2.HttpException) {
                onResult(extractError(e) ?: "Could not update your password.")
            } catch (e: Exception) {
                onResult("Could not update your password. Check your connection and try again.")
            }
        }
    }

    private fun extractError(e: retrofit2.HttpException): String? {
        val body = e.response()?.errorBody()?.string() ?: return null
        return try { com.google.gson.JsonParser.parseString(body).asJsonObject.get("error")?.asString } catch (_: Exception) { null }
    }

    private fun enterApp() {
        locationProvider.startUpdating()
        socket.connect()
        refreshPayoutStatus()
        refreshApplication()
        viewModelScope.launch {
            try { _fareRates.value = ApiClient.api.getFareRates() } catch (_: Exception) { /* estimate falls back below */ }
        }
    }

    // ---- Driver application ----
    fun refreshApplication() {
        val id = driverId ?: return
        viewModelScope.launch {
            try {
                val app = ApiClient.api.getApplication(id)
                _application.value = app
                _screen.value = if (app.status == "approved") AppScreen.DASHBOARD else AppScreen.APPLICATION
            } catch (_: Exception) { /* leave on current screen; user can retry */ }
        }
    }

    fun saveApplicationDraft(fields: Map<String, String>) {
        val id = driverId ?: return
        viewModelScope.launch {
            try {
                _application.value = ApiClient.api.updateApplication(id, fields)
            } catch (_: Exception) {
                _applicationError.value = "Could not save — check your connection and try again."
            }
        }
    }

    fun uploadDocument(docType: DocType, bytes: ByteArray, contentType: String) {
        val id = driverId ?: return
        viewModelScope.launch {
            _uploadingDoc.value = docType
            _applicationError.value = null
            try {
                val urlRes = ApiClient.api.getUploadUrl(id, mapOf("docType" to docType.key))
                val uploadUrl = urlRes.uploadUrl ?: throw Exception(urlRes.error ?: "no upload URL")
                ApiClient.uploadFile(uploadUrl, bytes, contentType)
                _application.value = ApiClient.api.confirmDocument(id, mapOf("docType" to docType.key, "key" to (urlRes.key ?: "")))
            } catch (_: Exception) {
                _applicationError.value = "Upload failed for ${docType.label} — try again."
            }
            _uploadingDoc.value = null
        }
    }

    fun submitApplication() {
        val id = driverId ?: return
        viewModelScope.launch {
            try {
                _application.value = ApiClient.api.submitApplication(id)
                _applicationError.value = null
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string()
                _applicationError.value = body?.let {
                    try { com.google.gson.JsonParser.parseString(it).asJsonObject.get("error")?.asString } catch (_: Exception) { null }
                } ?: "Please complete all required fields and documents before submitting."
            } catch (_: Exception) {
                _applicationError.value = "Could not submit application. Check your connection and try again."
            }
        }
    }

    private fun wireSocket() {
        socket.onConnect = {
            ApiClient.token?.let { socket.driverJoin(it) }
        }
        socket.onDriverState = { state ->
            _earnings.value = state.earnings
            _trips.value = state.trips
            state.history?.let { _history.value = it }
        }
        socket.onRequestNew = { ride -> showIncomingRequest(ride) }
        socket.onTripAssigned = { ride -> beginTrip(ride) }
        socket.onTripFinalized = { result -> showTripComplete(result) }
        socket.onOnlineRejected = { reason ->
            _online.value = false
            _applicationError.value = reason
        }
        // Pushed the moment an admin approves/rejects — updates immediately
        // instead of only on the next manual refresh.
        socket.onApplicationDecision = { status, reviewNotes ->
            _application.value = _application.value.copy(status = status, reviewNotes = reviewNotes)
            if (status == "approved") _screen.value = AppScreen.DASHBOARD
        }
        // Pushed immediately by an admin deactivating this account.
        socket.onAccountDeactivated = { reason ->
            _applicationError.value = reason
            logOut()
        }
        // A rider just rated this trip — refresh Insights quietly.
        socket.onTripRated = { refreshInsights() }
        // A rider just added a tip — reflect it in earnings immediately.
        socket.onTripTipped = { amount ->
            _earnings.value += amount
            refreshInsights()
        }
        // Crossed into a new reward tier — worth a moment of celebration.
        socket.onRewardTierUp = { tier, points ->
            _tierUpAlert.value = "You've reached $tier status! ($points points)"
            refreshInsights()
        }
    }

    // ---- Online toggle ----
    fun toggleOnline() {
        if (driverId == null) return
        val coord = currentCoordinate.value

        if (!_online.value) {
            if (coord == null) return // no GPS fix yet
            _online.value = true
            socket.driverOnline(coord.lat, coord.lng)
            _onlineSeconds.value = 0
            onlineTimerJob?.cancel()
            onlineTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _onlineSeconds.value += 1
                }
            }
        } else {
            _online.value = false
            socket.driverOffline()
            onlineTimerJob?.cancel()
        }
    }

    // ---- Incoming request ----
    private fun showIncomingRequest(ride: RideDTO) {
        _incomingRequest.value = ride
        _requestSecondsRemaining.value = 12
        requestTimerJob?.cancel()
        requestTimerJob = viewModelScope.launch {
            while (_requestSecondsRemaining.value > 0) {
                delay(1000)
                _requestSecondsRemaining.value -= 1
            }
            declineRequest()
        }
    }

    fun acceptRequest() {
        if (driverId == null) return
        val ride = _incomingRequest.value ?: return
        requestTimerJob?.cancel()
        socket.requestAccept(ride.requestId)
        _incomingRequest.value = null
    }

    fun declineRequest() {
        if (driverId == null) return
        val ride = _incomingRequest.value ?: return
        requestTimerJob?.cancel()
        socket.requestDecline(ride.requestId)
        _incomingRequest.value = null
    }

    // ---- Trip flow ----
    private fun beginTrip(ride: RideDTO) {
        _currentRide.value = ride
        _phase.value = TripPhase.TO_PICKUP
        _meterFare.value = 0.0
        _screen.value = AppScreen.TRIP

        val from = currentCoordinate.value ?: return
        viewModelScope.launch {
            val info = RoutingService.fetchRoute(from, ride.pickup)
            if (info != null) {
                _route.value = info
                _etaMinutes.value = info.durationMin.toInt().coerceAtLeast(1)
            }
        }
    }

    fun advanceTripPhase() {
        val ride = _currentRide.value ?: return
        when (_phase.value) {
            TripPhase.TO_PICKUP -> {
                _phase.value = TripPhase.READY_START
                socket.tripArrived(ride.requestId)
            }
            TripPhase.READY_START -> {
                _phase.value = TripPhase.IN_TRIP
                socket.tripStarted(ride.requestId)
                tripStartMillis = System.currentTimeMillis()

                viewModelScope.launch {
                    val info = RoutingService.fetchRoute(ride.pickup, ride.drop)
                    if (info != null) {
                        _route.value = info
                        tripDistanceKm = info.distanceKm
                        _etaMinutes.value = info.durationMin.toInt().coerceAtLeast(1)
                        startMeter(info.durationMin)
                    }
                }
            }
            TripPhase.IN_TRIP, TripPhase.READY_COMPLETE -> {
                meterJob?.cancel()
                if (driverId == null) return
                val durationMin = ((System.currentTimeMillis() - tripStartMillis) / 60000.0).coerceAtLeast(0.2)
                socket.tripComplete(ride.requestId, tripDistanceKm, durationMin)
            }
        }
    }

    /** Driver-initiated cancellation after accepting but before the rider is
     * actually in the car — counts toward cancelRate. Only valid during
     * TO_PICKUP; the UI hides the cancel button once the trip has started. */
    fun cancelTrip() {
        val ride = _currentRide.value ?: return
        if (_phase.value != TripPhase.TO_PICKUP) return
        socket.tripCancel(ride.requestId)
        _currentRide.value = null
        _route.value = null
        _screen.value = AppScreen.DASHBOARD
    }

    private fun startMeter(expectedDurationMin: Double) {
        meterJob?.cancel()
        meterJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsedMin = (System.currentTimeMillis() - tripStartMillis) / 60000.0
                val base = 2.5
                val distFare = tripDistanceKm * 0.95
                val progress = (elapsedMin / expectedDurationMin.coerceAtLeast(0.3)).coerceAtMost(1.0)
                val timeFare = elapsedMin * 0.22
                _meterFare.value = base + distFare * progress + timeFare
            }
        }
    }

    private fun showTripComplete(result: TripFinalizedResult) {
        meterJob?.cancel()
        _lastResult.value = result
        _earnings.value = result.driver.earnings
        _trips.value = result.driver.trips
        // Nested driver object omits history on this event — same as web/iOS.
        result.driver.history?.let { _history.value = it }
        _screen.value = AppScreen.COMPLETE
    }

    fun continueOnline() {
        _currentRide.value = null
        _route.value = null
        _screen.value = AppScreen.DASHBOARD
    }

    // ---- Payouts ----
    fun refreshPayoutStatus() {
        val id = driverId ?: return
        viewModelScope.launch {
            try {
                val status = ApiClient.api.getPayoutStatus(id)
                _payoutsConnected.value = status.connected
                _payoutsEnabled.value = status.payoutsEnabled
            } catch (_: Exception) { /* leave previous state */ }
        }
    }

    suspend fun startPayoutOnboarding(): String? {
        val id = driverId ?: return null
        return try {
            val response = ApiClient.api.getPayoutOnboardingLink(id, mapOf("returnOrigin" to "nexgendriver://"))
            response.url
        } catch (_: Exception) {
            null
        }
    }
}
