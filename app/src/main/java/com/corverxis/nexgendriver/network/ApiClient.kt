package com.corverxis.nexgendriver.network

import com.corverxis.nexgendriver.Config
import com.corverxis.nexgendriver.data.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NexgenApi {
    @POST("/api/auth/register")
    suspend fun register(@Body body: Map<String, String>): RegisterDriverResponse

    @POST("/api/auth/login")
    suspend fun login(@Body body: Map<String, String>): RegisterDriverResponse

    @GET("/api/auth/me")
    suspend fun me(): DriverState

    @POST("/api/auth/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): OkResponse

    @GET("/api/driver/{id}")
    suspend fun getDriver(@Path("id") id: String): DriverState

    @GET("/api/driver/{id}/stripe/status")
    suspend fun getPayoutStatus(@Path("id") id: String): PayoutStatus

    @POST("/api/driver/{id}/stripe/onboard-link")
    suspend fun getPayoutOnboardingLink(@Path("id") id: String, @Body body: Map<String, String>): OnboardLinkResponse

    @GET("/api/driver/{id}/application")
    suspend fun getApplication(@Path("id") id: String): DriverApplication

    @PUT("/api/driver/{id}/application")
    suspend fun updateApplication(@Path("id") id: String, @Body fields: Map<String, String>): DriverApplication

    @POST("/api/driver/{id}/application/documents/upload-url")
    suspend fun getUploadUrl(@Path("id") id: String, @Body body: Map<String, String>): UploadUrlResponse

    @POST("/api/driver/{id}/application/documents/confirm")
    suspend fun confirmDocument(@Path("id") id: String, @Body body: Map<String, String>): DriverApplication

    @POST("/api/driver/{id}/application/submit")
    suspend fun submitApplication(@Path("id") id: String): DriverApplication

    @GET("/api/driver/{id}/insights")
    suspend fun getInsights(@Path("id") id: String): DriverInsights

    // Public — no auth needed. Used to compute a fare estimate with the
    // exact same rates that will actually be charged.
    @GET("/api/fare/rates")
    suspend fun getFareRates(): FareRates
}

/** Attaches the driver's session token to every request. Set ApiClient.token
 * after login/register/session-restore — this is the single source of truth
 * for "who is this," not any ID passed around separately. */
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        ApiClient.token?.let { builder.addHeader("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}

object ApiClient {
    var token: String? = null

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()

    val api: NexgenApi by lazy {
        Retrofit.Builder()
            .baseUrl(Config.API_BASE + "/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NexgenApi::class.java)
    }

    private val uploadClient = OkHttpClient() // no auth header needed — the presigned URL is its own credential

    /** Uploads directly to S3 via the presigned PUT URL — the file never passes through our backend. */
    suspend fun uploadFile(uploadUrl: String, bytes: ByteArray, contentType: String) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = okhttp3.RequestBody.create(okhttp3.MediaType.parse(contentType), bytes)
            val request = okhttp3.Request.Builder().url(uploadUrl).put(body).build()
            uploadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Upload failed: ${response.code}")
            }
        }
}
