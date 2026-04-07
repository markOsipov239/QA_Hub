package qa_hub.service.integrations.tms.testrail

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import qa_hub.service.integrations.tms.qase.QaseHttpInterface
import qa_hub.service.integrations.tms.qase.QaseHttpInterface.Companion
import qa_hub.service.integrations.tms.testrail.entity.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.nio.charset.StandardCharsets

interface TestRailHttpInterface {
    companion object {
        var gson: Gson = GsonBuilder()
            .setLenient()
            .create()

        fun getClient(baseUrl: String, user: String, password: String): TestRailHttpInterface {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
                .addInterceptor { chain ->
                    val newRequest: Request = chain.request().newBuilder()
                        .addHeader(
                            "Authorization",  Credentials.basic(user, password, StandardCharsets.UTF_8)
                        )
                        .build()
                    chain.proceed(newRequest)
                }.build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()

            return retrofit.create(TestRailHttpInterface::class.java)
        }
    }

    @POST("/api/v2/add_run/{projectId}")
    fun addRun(
        @Path("projectId") projectId: String,
        @Body body: TestRailAddRunRequest
    ): Call<TestRailRunResponse>

    @GET("/api/v2/get_run/{runId}")
    fun getRun(@Path("runId") runId: String): Call<TestRailRunResponse>

    @POST("/api/v2/close_run/{runId}")
    fun closeRun(@Path("runId") runId: String): Call<TestRailRunResponse>

    @GET("/api/v2/get_case/{caseId}")
    fun getCases(
        @Path("caseId") caseId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<TestRailCasesPage>

    @POST("/api/v2/add_result_for_case/{runId}/{caseId}")
    fun addResultForCase(
        @Path("runId") runId: String,
        @Path("caseId") caseId: String,
        @Body body: TestRailAddResultRequest
    ): Call<TestRailAddResultResponse>
}
