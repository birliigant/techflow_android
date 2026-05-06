package com.birliigant.techflow.data.network

import com.birliigant.techflow.core.model.normalizeBaseUrl
import com.birliigant.techflow.data.repository.ConfigRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TechFlowApi {
    @GET("answer/api/v1/siteinfo")
    suspend fun getSiteInfo(): ApiEnvelope<SiteInfoDto>

    @GET("answer/api/v1/tags/page")
    suspend fun getTagsPage(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<List<TagSectionDto>>

    @GET("answer/api/v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("order") order: String = "relevance",
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<SearchItemDto>>

    @GET("answer/api/v1/question/page")
    suspend fun getQuestions(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("order") order: String? = null,
    ): ApiEnvelope<PageEnvelope<QuestionDto>>

    @GET("answer/api/v1/question/info")
    suspend fun getQuestionDetail(@Query("id") id: String): ApiEnvelope<QuestionDto>

    @GET("answer/api/v1/answer/page")
    suspend fun getAnswerPage(
        @Query("question_id") questionId: String,
        @Query("order") order: String = "default",
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<AnswerDto>>

    @GET("answer/api/v1/comment/page")
    suspend fun getCommentPage(
        @Query("object_id") objectId: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<CommentDto>>

    @GET("answer/api/v1/permission")
    suspend fun getPermission(@Query("action") action: String): ApiEnvelope<JsonObject?>

    @POST("answer/api/v1/vote/up")
    suspend fun voteUp(@Body request: VoteRequest): ApiEnvelope<VoteResponse>

    @POST("answer/api/v1/collection/switch")
    suspend fun switchCollection(@Body request: CollectionSwitchRequest): ApiEnvelope<CollectionSwitchResponse>

    @POST("answer/api/v1/comment")
    suspend fun addComment(@Body request: AddCommentRequest): ApiEnvelope<CommentDto>

    @POST("answer/api/v1/report")
    suspend fun addReport(@Body request: AddReportRequest): ApiEnvelope<JsonObject?>

    @POST("answer/api/v1/answer")
    suspend fun createAnswer(@Body request: CreateAnswerRequest): ApiEnvelope<JsonObject?>

    @HTTP(method = "DELETE", path = "answer/api/v1/question", hasBody = true)
    suspend fun deleteQuestion(@Body request: RemoveQuestionRequest): ApiEnvelope<JsonElement?>

    @HTTP(method = "DELETE", path = "answer/api/v1/answer", hasBody = true)
    suspend fun deleteAnswer(@Body request: RemoveAnswerRequest): ApiEnvelope<JsonElement?>

    @POST("answer/api/v1/user/login/email")
    suspend fun loginWithEmail(@Body request: EmailLoginRequest): ApiEnvelope<JsonObject>

    @POST("answer/api/v1/user/register/email")
    suspend fun registerWithEmail(@Body request: EmailRegisterRequest): ApiEnvelope<JsonObject?>

    @Multipart
    @POST("answer/api/v1/file")
    suspend fun uploadFile(
        @Part("source") source: RequestBody,
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<String>

    @GET("answer/api/v1/user/info")
    suspend fun getCurrentUser(): ApiEnvelope<UserDto?>

    @PUT("answer/api/v1/user/info")
    suspend fun updateCurrentUser(@Body request: UpdateUserInfoRequest): ApiEnvelope<JsonObject?>

    @PUT("answer/api/v1/user/profession")
    suspend fun updateProfession(@Body request: ChangeProfessionRequest): ApiEnvelope<JsonObject?>

    @GET("answer/api/v1/user/ranking")
    suspend fun getCommunityUsers(): ApiEnvelope<CommunityUsersDto>

    @GET("answer/api/v1/personal/user/info")
    suspend fun getPublicUserProfile(@Query("username") username: String): ApiEnvelope<JsonElement>

    @GET("answer/api/v1/personal/question/page")
    suspend fun getPersonalQuestionPage(
        @Query("username") username: String,
        @Query("order") order: String = "newest",
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<QuestionDto>>

    @GET("answer/api/v1/personal/answer/page")
    suspend fun getPersonalAnswerPage(
        @Query("username") username: String,
        @Query("order") order: String = "newest",
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<AnswerDto>>

    @GET("answer/api/v1/personal/collection/page")
    suspend fun getPersonalCollectionPage(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<QuestionDto>>

    @GET("answer/api/v1/personal/comment/page")
    suspend fun getPersonalCommentPage(
        @Query("username") username: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<PersonalCommentDto>>

    @GET("answer/api/v1/personal/rank/page")
    suspend fun getPersonalRankPage(
        @Query("username") username: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<PersonalRankDto>>

    @GET("answer/api/v1/personal/vote/page")
    suspend fun getPersonalVotePage(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiEnvelope<PageEnvelope<PersonalVoteDto>>

    @GET("answer/api/v1/badge/user/awards")
    suspend fun getUserBadgeAwards(
        @Query("username") username: String,
    ): ApiEnvelope<JsonElement>

    @GET("answer/api/v1/badge/user/awards/recent")
    suspend fun getRecentUserBadgeAwards(
        @Query("username") username: String,
    ): ApiEnvelope<JsonElement>

    @GET("answer/api/v1/user/logout")
    suspend fun logout(): ApiEnvelope<JsonObject?>

    @Headers("$SHORT_TIMEOUT_HEADER: true")
    @POST("answer/api/v1/question")
    suspend fun createQuestion(@Body request: CreateQuestionRequest): ApiEnvelope<JsonObject?>
}

private const val SHORT_TIMEOUT_HEADER = "X-TechFlow-Short-Timeout"

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
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val useShortTimeout = request.header(SHORT_TIMEOUT_HEADER) == "true"
                        val builder = request.newBuilder()
                            .removeHeader(SHORT_TIMEOUT_HEADER)
                        if (token.isNotBlank()) {
                            builder.header("Authorization", token)
                        }
                        val next = if (useShortTimeout) {
                            chain
                                .withConnectTimeout(12, TimeUnit.SECONDS)
                                .withReadTimeout(12, TimeUnit.SECONDS)
                                .withWriteTimeout(12, TimeUnit.SECONDS)
                        } else {
                            chain
                        }
                        next.proceed(builder.build())
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
