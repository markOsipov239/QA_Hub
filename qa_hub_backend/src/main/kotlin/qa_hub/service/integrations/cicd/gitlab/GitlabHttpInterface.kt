package qa_hub.service.integrations.cicd.gitlab

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabBranch
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabPipeline
import qa_hub.service.integrations.cicd.gitlab.entity.GitlabStartPipelineRequest
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface GitlabHttpInterface {
    companion object {
        var gson: Gson = GsonBuilder()
            .setLenient()
            .create()

        fun getClient(apiUrl: String, apiToken: String): GitlabHttpInterface {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
                .addInterceptor { chain ->
                    val newRequest: Request = chain.request().newBuilder()
                        .addHeader(
                            "Authorization", "Bearer $apiToken"
                        )
                        .build()
                    chain.proceed(newRequest)
                }.build()

            val retrofit = Retrofit.Builder()
                .baseUrl(apiUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()

            return retrofit.create(GitlabHttpInterface::class.java)
        }
    }

    @GET("/api/v4/projects/{projectId}/repository/branches")
    fun getBranches(
        @Path("projectId") projectId: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): Call<List<GitlabBranch>>

    @POST("/api/v4/projects/{projectId}/pipeline")
    fun startPipeline(
        @Path("projectId") project: String,
        @Body body: GitlabStartPipelineRequest
    ): Call<GitlabPipeline?>

    @POST("/api/v4/projects/{projectId}/pipelines/{pipelineId}/cancel")
    fun cancelPipeline(
        @Path("projectId") projectId: String,
        @Path("pipelineId") pipelineId: String
    ): Call<Any?>
}