package com.corverxis.nexgendriver.data

data class Coordinate(
    val lat: Double,
    val lng: Double
)

data class TripHistoryItem(
    val pickupLabel: String?,
    val dropLabel: String?,
    val fare: Double,
    val time: Double,
    val driverPayout: Double?,
    val paymentStatus: String?
)

data class DriverState(
    val id: String,
    val name: String,
    val online: Boolean,
    val lat: Double?,
    val lng: Double?,
    val earnings: Double,
    val trips: Int,
    // Absent on the trip:finalized payload's nested driver object — same
    // quirk as the web app and iOS versions. Always null-check this.
    val history: List<TripHistoryItem>?,
    val payoutsEnabled: Boolean?
)

/** Matches toRideDTO() on the backend. */
data class RideDTO(
    val rideId: String,
    val requestId: String,
    val riderId: String,
    val driverId: String?,
    val pickup: Coordinate,
    val drop: Coordinate,
    val pickupLabel: String?,
    val dropLabel: String?,
    val estDistanceKm: Double?,
    val rider: String?,
    val status: String,
    val dispatchMode: String? = null // "direct" | "matched"
)

data class TripFinalizedResult(
    val total: Double,
    val base: Double,
    val distFare: Double,
    val timeFare: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val driver: DriverState,
    val paymentStatus: String,
    val driverPayout: Double,
    val platformFee: Double,
    val insuranceFee: Double,
    val tripPoints: Int? = null,
    val isBonusTrip: Boolean? = null,
    val gasPricePerGallon: Double? = null,
    val gasPriceSource: String? = null,
    val fuelCostPerKm: Double? = null
)

/** Mirrors GET /api/fare/rates — the single source of truth for computing a
 * fare estimate that matches what will actually be charged, instead of a
 * separately-maintained guess that can drift from real billing (which is
 * exactly what was happening here before: this screen used to compute its
 * own estimate with a different, made-up formula). */
data class FareRates(
    val base: Double,
    val nonFuelPerKm: Double,
    val perMin: Double,
    val fuelCostPerKm: Double,
    val perKmEffective: Double,
    val gasPricePerGallon: Double,
    val gasPriceSource: String,
    val assumedMpg: Double,
    val gasPriceFetchedAt: Double
)

data class PayoutStatus(
    val connected: Boolean,
    val payoutsEnabled: Boolean,
    val detailsSubmitted: Boolean?
)

data class AuthDriver(
    val id: String,
    val name: String,
    val email: String
)

data class RegisterDriverResponse(
    val driverId: String,
    val token: String,
    val driver: AuthDriver
)

data class AuthErrorResponse(
    val error: String?
)

data class OkResponse(
    val ok: Boolean?,
    val error: String?
)

data class OnboardLinkResponse(
    val url: String?,
    val error: String?
)

// ---------------------------------------------------------------------
// Driver application — mirrors DriverApplication in the Prisma schema.
// Deliberately no SSN field — Checkr's hosted page collects that directly,
// never this app or its backend.
// ---------------------------------------------------------------------
enum class DocType(val key: String, val label: String) {
    LICENSE_FRONT("licenseFront", "Driver's license — front"),
    LICENSE_BACK("licenseBack", "Driver's license — back"),
    INSURANCE_DOC("insuranceDoc", "Insurance declarations page"),
    REGISTRATION_DOC("registrationDoc", "Vehicle registration"),
    VEHICLE_PHOTO_FRONT("vehiclePhotoFront", "Vehicle photo — front"),
    VEHICLE_PHOTO_BACK("vehiclePhotoBack", "Vehicle photo — back"),
    VEHICLE_PHOTO_LEFT("vehiclePhotoLeft", "Vehicle photo — left side"),
    VEHICLE_PHOTO_RIGHT("vehiclePhotoRight", "Vehicle photo — right side"),
}

data class DocUrls(
    val licenseFront: String?,
    val licenseBack: String?,
    val insuranceDoc: String?,
    val registrationDoc: String?,
    val vehiclePhotoFront: String?,
    val vehiclePhotoBack: String?,
    val vehiclePhotoLeft: String?,
    val vehiclePhotoRight: String?
) {
    operator fun get(type: DocType): String? = when (type) {
        DocType.LICENSE_FRONT -> licenseFront
        DocType.LICENSE_BACK -> licenseBack
        DocType.INSURANCE_DOC -> insuranceDoc
        DocType.REGISTRATION_DOC -> registrationDoc
        DocType.VEHICLE_PHOTO_FRONT -> vehiclePhotoFront
        DocType.VEHICLE_PHOTO_BACK -> vehiclePhotoBack
        DocType.VEHICLE_PHOTO_LEFT -> vehiclePhotoLeft
        DocType.VEHICLE_PHOTO_RIGHT -> vehiclePhotoRight
    }
}

data class DriverApplication(
    val id: String,
    val status: String, // draft | submitted | under_review | approved | rejected

    val legalFirstName: String?,
    val legalLastName: String?,
    val dateOfBirth: String?,
    val phone: String?,
    val email: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val zip: String?,

    val licenseNumber: String?,
    val licenseState: String?,
    val licenseExpiration: String?,

    val insuranceProvider: String?,
    val insurancePolicyNum: String?,
    val insuranceExpiration: String?,

    val vehicleMake: String?,
    val vehicleModel: String?,
    val vehicleYear: String?,
    val vehicleColor: String?,
    val licensePlate: String?,
    val vin: String?,

    val checkrInvitationUrl: String?,
    val checkrStatus: String?,
    val reviewNotes: String?,

    val docUrls: DocUrls?
) {
    companion object {
        val EMPTY = DriverApplication(
            id = "", status = "draft", legalFirstName = null, legalLastName = null, dateOfBirth = null,
            phone = null, email = null, addressLine1 = null, addressLine2 = null, city = null, state = null, zip = null,
            licenseNumber = null, licenseState = null, licenseExpiration = null,
            insuranceProvider = null, insurancePolicyNum = null, insuranceExpiration = null,
            vehicleMake = null, vehicleModel = null, vehicleYear = null, vehicleColor = null, licensePlate = null, vin = null,
            checkrInvitationUrl = null, checkrStatus = null, reviewNotes = null,
            docUrls = DocUrls(null, null, null, null, null, null, null, null)
        )
    }
}

data class UploadUrlResponse(
    val uploadUrl: String?,
    val key: String?,
    val error: String?
)

// ---------------------------------------------------------------------
// Insights — rating, acceptance rate, cancel rate, rider comments.
// ---------------------------------------------------------------------
data class RiderComment(
    val rating: Int?,
    val comment: String?,
    val riderName: String?,
    val date: Double
)

data class RiderTip(
    val amount: Double,
    val riderName: String?,
    val date: Double
)

data class RewardTier(
    val name: String,
    val threshold: Int
)

data class DriverInsights(
    val avgRating: Double?,
    val ratingCount: Int,
    val acceptanceRate: Double?,
    val cancelRate: Double?,
    val requestsReceived: Int,
    val requestsAccepted: Int,
    val requestsDeclined: Int,
    val tripsCancelled: Int,
    val totalTips: Double,
    val tipCount: Int,
    val recentTips: List<RiderTip>,
    val recentComments: List<RiderComment>,
    val rewardPoints: Int,
    val rewardTier: String?,
    val nextRewardTier: String?,
    val pointsToNextTier: Int?,
    val rewardTiers: List<RewardTier>
)
