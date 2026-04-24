package com.birliigant.techflow.data.network

import com.birliigant.techflow.core.model.normalizeBaseUrl
import com.birliigant.techflow.data.repository.ConfigRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TechFlowApi {
    @GET("answer/api/v1/siteinfo")
    suspend fun getSiteInfo(): ApiEnvelope<SiteInfoDto>

    @GET("answer/api/v1/question/page")
    suspend fun getQuestions(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<QuestionDto>>

    @GET("answer/api/v1/question/info")
    suspend fun getQuestionDetail(@Query("id") id: String): ApiEnvelope<QuestionDto>

    @GET("answer/api/v1/answer/page")
    suspend fun getAnswerPage(
        @Query("question_id") questionId: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<AnswerDto>>

    @GET("answer/api/v1/comment/page")
    suspend fun getCommentPage(
        @Query("object_id") objectId: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<CommentDto>>

    @POST("answer/api/v1/user/login/email")
    suspend fun loginWithEmail(@Body request: EmailLoginRequest): ApiEnvelope<JsonObject>

    @GET("answer/api/v1/user/info")
    suspend fun getCurrentUser(): ApiEnvelope<UserDto?>

    @GET("answer/api/v1/user/logout")
    suspend fun logout(): ApiEnvelope<JsonObject?>

    @POST("answer/api/v1/question")
    suspend fun createQuestion(@Body request: CreateQuestionRequest): ApiEnvelope<JsonObject?>
}

class ApiClientProvider(
    private val configRepository: ConfigRepository,
    private val sessionRepository: SessionRepository,
    private val gson: Gson,
) {
    @Volatile
    private var cachedKey: String? = null

    @Volatile
    private var cachedApi: TechFlowApi? = null

    fun api(): TechFlowApi {
        val baseUrl = normalizeBaseUrl(configRepository.baseUrl.value)
        val token = sessionRepository.token.value
        val key = "$baseUrl|$token"

        val existing = cachedApi
        if (existing != null && cachedKey == key) {
            return existing
        }

        return synchronized(this) {
            val current = cachedApi
            if (current != null && cachedKey == key) {
                current
            } else {
                val client = OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val builder = chain.request().newBuilder()
                        if (token.isNotBlank()) {
                            builder.header("Authorization", "Bearer $token")
                        }
                        chain.proceed(builder.build())
                    }
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build()

                retrofit.create(TechFlowApi::class.java).also {
                    cachedKey = key
                    cachedApi = it
                }
            }
        }
    }
}
