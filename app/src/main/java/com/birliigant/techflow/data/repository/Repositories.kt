package com.birliigant.techflow.data.repository

import com.birliigant.techflow.core.model.AppDefaults
import com.birliigant.techflow.core.model.BadgeAward
import com.birliigant.techflow.core.model.CommunityUser
import com.birliigant.techflow.core.model.PersonalCommentActivity
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.core.model.QuestionDraft
import com.birliigant.techflow.core.model.SearchPostItem
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.ReputationActivity
import com.birliigant.techflow.core.model.SiteInfo
import com.birliigant.techflow.core.model.TagSection
import com.birliigant.techflow.core.model.TagItem
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.core.model.UserProfileUpdate
import com.birliigant.techflow.core.model.VoteActivity
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.core.model.markdownPreview
import com.birliigant.techflow.core.model.normalizeBaseUrl
import com.birliigant.techflow.data.local.CachedQuestionEntity
import com.birliigant.techflow.data.local.QuestionDao
import com.birliigant.techflow.data.network.ApiClientProvider
import com.birliigant.techflow.data.network.ApiEnvelope
import com.birliigant.techflow.data.network.AddCommentRequest
import com.birliigant.techflow.data.network.AddReportRequest
import com.birliigant.techflow.data.network.CollectionSwitchRequest
import com.birliigant.techflow.data.network.CreateAnswerRequest
import com.birliigant.techflow.data.network.EmailRegisterRequest
import com.birliigant.techflow.data.network.EmailLoginRequest
import com.birliigant.techflow.data.network.RemoveAnswerRequest
import com.birliigant.techflow.data.network.RemoveQuestionRequest
import com.birliigant.techflow.data.network.normalizeRemoteUrl
import com.birliigant.techflow.data.network.toBadgeAwardList
import com.birliigant.techflow.data.network.toDetail
import com.birliigant.techflow.data.network.toInfoRequest
import com.birliigant.techflow.data.network.toModel
import com.birliigant.techflow.data.network.toPublicUserProfile
import com.birliigant.techflow.data.network.toQuestionSummaryOrNull
import com.birliigant.techflow.data.network.toSearchPostOrNull
import com.birliigant.techflow.data.network.toProfessionRequest
import com.birliigant.techflow.data.network.toRequest
import com.birliigant.techflow.data.network.toSummary
import com.birliigant.techflow.data.network.VoteRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.ZoneId

class ConfigRepository(private val storage: MMKV) {
    init {
        // The app no longer exposes custom endpoint settings, so discard any stale
        // base URL persisted by older builds to avoid routing users to dead intranet hosts.
        storage.removeValueForKey(KEY_BASE_URL)
    }

    private val _baseUrl = MutableStateFlow(AppDefaults.defaultBaseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    fun saveBaseUrl(raw: String) {
        val normalized = normalizeBaseUrl(raw)
        val forcedUrl = AppDefaults.defaultBaseUrl
        if (normalized != forcedUrl) {
            storage.removeValueForKey(KEY_BASE_URL)
        } else {
            storage.encode(KEY_BASE_URL, forcedUrl)
        }
        _baseUrl.value = forcedUrl
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
    }
}

class UiPreferenceRepository(private val storage: MMKV) {
    fun hasSeenSearchHints(): Boolean = storage.decodeBool(KEY_SEEN_SEARCH_HINTS, false)

    fun markSearchHintsSeen() {
        storage.encode(KEY_SEEN_SEARCH_HINTS, true)
    }

    companion object {
        private const val KEY_SEEN_SEARCH_HINTS = "seen_search_hints"
    }
}

class SessionRepository(
    private val storage: MMKV,
    private val gson: Gson,
) {
    private val _token = MutableStateFlow(storage.decodeString(KEY_TOKEN).orEmpty())
    private val _currentUser = MutableStateFlow(storage.decodeString(KEY_USER)?.let(::decodeStoredUser))

    val token: StateFlow<String> = _token.asStateFlow()
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun setToken(value: String) {
        storage.encode(KEY_TOKEN, value)
        _token.value = value
    }

    fun clearToken() {
        storage.removeValueForKey(KEY_TOKEN)
        _token.value = ""
    }

    fun setCurrentUser(user: UserProfile?) {
        if (user == null) {
            storage.removeValueForKey(KEY_USER)
        } else {
            storage.encode(KEY_USER, gson.toJson(user))
        }
        _currentUser.value = user
    }

    fun clearSession() {
        clearToken()
        setCurrentUser(null)
    }

    private fun decodeStoredUser(raw: String): UserProfile? {
        val json = runCatching { gson.fromJson(raw, JsonObject::class.java) }.getOrNull() ?: return null
        fun str(key: String): String = json.get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        fun int(key: String): Int = json.get(key)?.takeIf { !it.isJsonNull }?.asInt ?: 0

        val username = str("username").ifBlank { "anonymous" }
        return UserProfile(
            id = str("id"),
            username = username,
            displayName = str("displayName").ifBlank { str("display_name") }.ifBlank { username },
            email = str("email").ifBlank { str("e_mail") },
            avatar = str("avatar").normalizeRemoteUrl().ifBlank { null },
            rank = int("rank"),
            questionCount = int("questionCount").takeIf { it != 0 } ?: int("question_count"),
            answerCount = int("answerCount").takeIf { it != 0 } ?: int("answer_count"),
            followCount = int("followCount").takeIf { it != 0 } ?: int("follow_count"),
            bio = str("bio"),
            website = str("website"),
            location = str("location"),
            profession = str("profession"),
        )
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USER = "current_user"
    }
}

class SiteRepository(
    private val apiClientProvider: ApiClientProvider,
) {
    suspend fun getSiteInfo(): Result<SiteInfo> = runCatching {
        val envelope = apiClientProvider.api().getSiteInfo()
        envelope.requireData().toModel()
    }
}

class TagRepository(
    private val apiClientProvider: ApiClientProvider,
) {
    suspend fun getTagSections(
        page: Int = 1,
        pageSize: Int = 50,
    ): Result<List<TagSection>> = runCatching {
        apiClientProvider.api().getTagsPage(page, pageSize).requireData().map { it.toModel() }
    }

    suspend fun getAllTags(): Result<List<com.birliigant.techflow.core.model.TagDetail>> = runCatching {
        getTagSections(page = 1, pageSize = 100).getOrThrow()
            .flatMap { it.tags }
            .distinctBy { it.slug.ifBlank { it.name } }
    }
}

class QuestionRepository(
    private val apiClientProvider: ApiClientProvider,
    private val questionDao: QuestionDao,
) {
    suspend fun searchPosts(
        query: String,
        order: String = "relevance",
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<SearchPostItem>> = runCatching {
        apiClientProvider.api()
            .search(query = query, order = order, page = page, pageSize = pageSize)
            .requireData()
            .list
            .orEmpty()
            .mapNotNull { it.toSearchPostOrNull() }
    }

    suspend fun searchQuestions(
        query: String,
        order: String = "relevance",
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<QuestionSummary>> = runCatching {
        searchPosts(query = query, order = order, page = page, pageSize = pageSize)
            .getOrThrow()
            .filter { it.objectType == "question" }
            .map {
                QuestionSummary(
                    id = it.questionId,
                    title = it.title,
                    excerpt = it.excerpt,
                    authorName = it.authorName,
                    authorUsername = it.authorUsername,
                    authorAvatar = it.authorAvatar,
                    answerCount = it.answerCount,
                    voteCount = it.voteCount,
                    viewCount = it.viewCount,
                    createdAt = it.createdAt,
                    tags = it.tags,
                    accepted = it.accepted,
                )
            }
    }

    suspend fun getQuestionPage(
        page: Int = 1,
        pageSize: Int = 20,
        order: String? = null,
    ): Result<List<QuestionSummary>> {
        return runCatching {
            val envelope = apiClientProvider.api().getQuestions(page, pageSize, order)
            val questions = envelope.requireData().list.orEmpty().map { it.toSummary() }
            cacheQuestions(questions)
            questions
        }.recoverCatching {
            val cached = questionDao.getAll().map { it.toModel() }
            if (cached.isNotEmpty()) cached else throw it
        }
    }

    suspend fun getQuestionDetail(questionId: String): Result<QuestionDetail> {
        return runCatching {
            coroutineScope {
                val api = apiClientProvider.api()
                val question = api.getQuestionDetail(questionId).requireData()
                val answers = runCatching {
                    api.getAnswerPage(questionId = questionId, order = "default", page = 1, pageSize = 20)
                        .requireData()
                        .list
                        .orEmpty()
                        .map { it.toModel() }
                }.getOrDefault(emptyList())
                val comments = runCatching {
                    api.getCommentPage(questionId, 1, 20).requireData().list.orEmpty().map { it.toModel() }
                }.getOrDefault(emptyList())
                question.toDetail(answers = answers, comments = comments)
            }
        }.recoverCatching { error ->
            questionDao.getById(questionId)?.toDetailFallback() ?: throw error
        }
    }

    suspend fun createQuestion(draft: QuestionDraft): Result<String> = runCatching {
        val response = apiClientProvider.api().createQuestion(draft.toRequest())
        val payload = response.requireNullableData()
        payload.extractString("id", "question_id", "questionId").orEmpty()
    }.recoverCatching { throwable ->
        val recoveredId = if (throwable.isUncertainCreateQuestionFailure()) {
            findRecentlySubmittedQuestion(draft)
        } else {
            null
        }
        if (!recoveredId.isNullOrBlank()) {
            recoveredId
        } else {
            throw throwable.toApiMessageException("提交问题失败，请检查标题、正文和标签后再试")
        }
    }

    private suspend fun findRecentlySubmittedQuestion(draft: QuestionDraft): String? {
        return runCatching {
            val title = draft.title.trim()
            val contentPreview = markdownPreview(draft.content, maxLength = 80)
            val today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString()
            apiClientProvider.api()
                .getQuestions(page = 1, pageSize = 10, order = "newest")
                .requireData()
                .list
                .orEmpty()
                .map { it.toSummary() }
                .firstOrNull { question ->
                    question.title.trim() == title &&
                        formatDisplayDate(question.createdAt) == today &&
                        question.matchesDraftContent(contentPreview)
                }
                ?.id
        }.getOrNull()
    }

    private fun Throwable.isUncertainCreateQuestionFailure(): Boolean {
        return this is SocketTimeoutException || (this as? HttpException)?.code() == 400
    }

    private fun QuestionSummary.matchesDraftContent(contentPreview: String): Boolean {
        if (contentPreview.isBlank()) return true
        val previewProbe = contentPreview.take(24)
        val excerptProbe = excerpt.take(24)
        return previewProbe.isBlank() ||
            excerptProbe.isBlank() ||
            excerpt.contains(previewProbe, ignoreCase = true) ||
            contentPreview.contains(excerptProbe, ignoreCase = true)
    }

    suspend fun uploadPostImage(
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String> = runCatching {
        val sourceBody = "post".toRequestBody("text/plain".toMediaType())
        val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
        apiClientProvider.api()
            .uploadFile(source = sourceBody, file = filePart)
            .requireData()
            .normalizeRemoteUrl()
    }

    suspend fun getCommentsForObject(
        objectId: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<com.birliigant.techflow.core.model.CommentItem>> = runCatching {
        apiClientProvider.api()
            .getCommentPage(objectId = objectId, page = page, pageSize = pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toModel() }
    }

    suspend fun toggleVoteUp(
        objectId: String,
        cancel: Boolean,
        permissionAction: String,
    ): Result<Pair<Int, String>> = runCatching {
        if (!cancel) {
            ensurePermission(permissionAction)
        }
        val response = apiClientProvider.api().voteUp(
            VoteRequest(
                objectId = objectId,
                isCancel = cancel,
            ),
        ).requireData()
        val votes = response.votes ?: response.upVotes ?: 0
        votes to response.voteStatus.orEmpty()
    }.recoverCatching { throwable ->
        throw throwable.toApiMessageException(
            fallback = "点赞失败，请稍后再试",
            forbiddenFallback = "当前账号暂无点赞权限，或不能给自己的内容点赞",
        )
    }

    private suspend fun ensurePermission(action: String) {
        val permission = apiClientProvider.api().getPermission(action).requireNullableData()
        if (permission.isPermissionAllowed(action) == false) {
            error(permission.permissionTip(action) ?: "当前账号暂无点赞权限")
        }
    }

    suspend fun toggleCollection(
        objectId: String,
        collected: Boolean,
    ): Result<Int> = runCatching {
        apiClientProvider.api().switchCollection(
            CollectionSwitchRequest(
                objectId = objectId,
                bookmark = !collected,
            ),
        ).requireData().objectCollectionCount ?: 0
    }

    suspend fun addComment(
        objectId: String,
        content: String,
        replyCommentId: String? = null,
        mentionUsernameList: List<String>? = null,
    ): Result<com.birliigant.techflow.core.model.CommentItem> = runCatching {
        apiClientProvider.api().addComment(
            AddCommentRequest(
                objectId = objectId,
                originalText = content,
                replyCommentId = replyCommentId,
                mentionUsernameList = mentionUsernameList,
            ),
        ).requireData().toModel()
    }

    suspend fun reportObject(
        objectId: String,
        content: String,
        reportType: Int = 1,
    ): Result<Unit> = runCatching {
        apiClientProvider.api().addReport(
            AddReportRequest(
                objectId = objectId,
                reportType = reportType,
                content = content.ifBlank { null },
            ),
        ).requireNullableData()
    }

    suspend fun createAnswer(
        questionId: String,
        content: String,
    ): Result<Unit> = runCatching {
        apiClientProvider.api().createAnswer(
            CreateAnswerRequest(
                questionId = questionId,
                content = content,
            ),
        ).requireNullableData()
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> = runCatching {
        apiClientProvider.api()
            .deleteQuestion(RemoveQuestionRequest(id = questionId))
            .requireNullableData()
        Unit
    }.recoverCatching { throwable ->
        throw throwable.toApiMessageException("删除帖子失败，请稍后再试")
    }

    suspend fun deleteAnswer(answerId: String): Result<Unit> = runCatching {
        apiClientProvider.api()
            .deleteAnswer(RemoveAnswerRequest(id = answerId))
            .requireNullableData()
        Unit
    }.recoverCatching { throwable ->
        throw throwable.toApiMessageException("删除回答失败，请稍后再试")
    }

    private suspend fun cacheQuestions(questions: List<QuestionSummary>) {
        val cached = questions.map { item ->
            CachedQuestionEntity(
                id = item.id,
                title = item.title,
                excerpt = item.excerpt,
                authorName = item.authorName,
                answerCount = item.answerCount,
                voteCount = item.voteCount,
                viewCount = item.viewCount,
                createdAt = item.createdAt,
                tags = item.tags.map(TagItem::name),
                accepted = item.accepted,
                syncedAt = System.currentTimeMillis(),
            )
        }
        questionDao.clearAll()
        questionDao.upsertAll(cached)
    }
}

class UserRepository(
    private val apiClientProvider: ApiClientProvider,
    private val sessionRepository: SessionRepository,
) {
    suspend fun refreshCurrentUser(): Result<UserProfile?> = runCatching {
        val envelope = apiClientProvider.api().getCurrentUser()
        val user = envelope.requireNullableData()?.toModel()
        sessionRepository.setCurrentUser(user)
        user
    }

    suspend fun loginWithEmail(
        email: String,
        password: String,
    ): Result<UserProfile?> = runCatching {
        val response = apiClientProvider.api().loginWithEmail(
            EmailLoginRequest(email = email, password = password),
        )
        val token = response.requireData().extractString(
            "access_token",
            "accessToken",
            "token",
            "visit_token",
        ).orEmpty()

        require(token.isNotBlank()) {
            "登录成功，但暂时无法完成账号授权，请稍后重试。"
        }

        sessionRepository.setToken(token)
        refreshCurrentUser().getOrThrow()
    }

    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        profession: String,
    ): Result<Unit> = runCatching {
        apiClientProvider.api().registerWithEmail(
            EmailRegisterRequest(
                name = name,
                email = email,
                password = password,
                profession = profession.ifBlank { null },
            ),
        ).requireNullableData()
    }

    suspend fun updateProfile(update: UserProfileUpdate): Result<UserProfile?> = runCatching {
        apiClientProvider.api().updateCurrentUser(update.toInfoRequest()).requireNullableData()
        apiClientProvider.api().updateProfession(update.toProfessionRequest()).requireNullableData()
        refreshCurrentUser().getOrThrow()
    }

    suspend fun getCommunityUsers(): Result<List<CommunityUser>> = runCatching {
        val payload = apiClientProvider.api().getCommunityUsers().requireData()
        buildList {
            addAll(payload.staffs.orEmpty())
            addAll(payload.topReputationUsers.orEmpty())
            addAll(payload.topVoteUsers.orEmpty())
        }.map { it.toModel() }.distinctBy { it.username }
    }

    suspend fun getPublicProfile(username: String): Result<PublicUserProfile> = runCatching {
        apiClientProvider.api()
            .getPublicUserProfile(username)
            .requireData()
            .toPublicUserProfile()
            ?: error("服务端没有返回有效的用户主页信息")
    }

    suspend fun getPersonalQuestions(
        username: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<QuestionSummary>> = runCatching {
        apiClientProvider.api()
            .getPersonalQuestionPage(username = username, order = "newest", page = page, pageSize = pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toSummary() }
    }

    suspend fun getPersonalAnswers(
        username: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<com.birliigant.techflow.core.model.AnswerItem>> = runCatching {
        apiClientProvider.api()
            .getPersonalAnswerPage(username = username, order = "newest", page = page, pageSize = pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toModel() }
    }

    suspend fun getPersonalCollections(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<QuestionSummary>> = runCatching {
        apiClientProvider.api()
            .getPersonalCollectionPage(page, pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toSummary() }
    }

    suspend fun getPersonalComments(
        username: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<PersonalCommentActivity>> = runCatching {
        apiClientProvider.api()
            .getPersonalCommentPage(username, page, pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toModel() }
    }

    suspend fun getPersonalRanks(
        username: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<ReputationActivity>> = runCatching {
        apiClientProvider.api()
            .getPersonalRankPage(username, page, pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toModel() }
    }

    suspend fun getPersonalVotes(
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<List<VoteActivity>> = runCatching {
        apiClientProvider.api()
            .getPersonalVotePage(page, pageSize)
            .requireData()
            .list
            .orEmpty()
            .map { it.toModel() }
    }

    suspend fun getUserBadgeAwards(
        username: String,
        recentOnly: Boolean = false,
    ): Result<List<BadgeAward>> = runCatching {
        val response = if (recentOnly) {
            apiClientProvider.api().getRecentUserBadgeAwards(username)
        } else {
            apiClientProvider.api().getUserBadgeAwards(username)
        }
        response.requireData().toBadgeAwardList()
    }

    suspend fun logout() {
        runCatching { apiClientProvider.api().logout() }
        sessionRepository.clearSession()
    }
}

private fun <T> ApiEnvelope<T>.requireData(): T {
    require((code ?: 200) in 200..299) { msg ?: reason ?: "请求失败" }
    return data ?: error(msg ?: reason ?: "服务端没有返回数据")
}

private fun <T> ApiEnvelope<T?>.requireNullableData(): T? {
    require((code ?: 200) in 200..299) { msg ?: reason ?: "请求失败" }
    return data
}

private fun JsonObject?.isPermissionAllowed(action: String): Boolean? {
    val source = this ?: return null
    source.booleanOrNull(action)?.let { return it }
    return source.permissionPayloads(action).firstNotNullOfOrNull { payload ->
        payload.booleanOrNull("has_permission")
            ?: payload.booleanOrNull("permission")
            ?: payload.booleanOrNull("allowed")
    }
}

private fun JsonObject?.permissionTip(action: String? = null): String? {
    val source = this ?: return null
    return source.permissionPayloads(action).firstNotNullOfOrNull { payload ->
        payload.stringOrNull("no_permission_tip")
            ?: payload.stringOrNull("msg")
            ?: payload.stringOrNull("reason")
    }?.toUserFriendlyMessage()
}

private fun JsonObject.permissionPayloads(action: String?): List<JsonObject> {
    val payloads = buildList {
        action?.let { objectOrNull(it)?.let(::add) }
        objectOrNull("permission")?.let(::add)
        objectOrNull("data")?.let(::add)
        add(this@permissionPayloads)
    }
    return payloads.distinct()
}

private fun JsonObject.objectOrNull(key: String): JsonObject? {
    return runCatching {
        get(key)?.takeIf { !it.isJsonNull && it.isJsonObject }?.asJsonObject
    }.getOrNull()
}

private fun JsonObject.stringOrNull(key: String): String? {
    return runCatching {
        get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

private fun JsonObject.booleanOrNull(key: String): Boolean? {
    return runCatching {
        get(key)?.takeIf { !it.isJsonNull }?.asBoolean
    }.getOrNull()
}

private fun Throwable.toApiMessageException(
    fallback: String,
    forbiddenFallback: String = fallback,
): Throwable {
    if (this !is HttpException) return this
    val raw = response()?.errorBody()?.string()
    val parsedMessage = raw?.let { body ->
        runCatching {
            Gson().fromJson(body, JsonObject::class.java)
                ?.let { it.stringOrNull("msg") ?: it.stringOrNull("reason") }
        }.getOrNull()
    }
    val message = parsedMessage?.toUserFriendlyMessage() ?: if (code() == 403) forbiddenFallback else fallback
    return IllegalStateException(message, this)
}

private fun String.toUserFriendlyMessage(): String {
    val reputation = Regex("You need at least (\\d+) reputation to do this\\.", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
    if (reputation != null) {
        return "当前账号声望至少需要 $reputation 才能点赞"
    }
    if (contains("can't vote for your own", ignoreCase = true)) {
        return "不能给自己的内容点赞"
    }
    return this
}

private fun JsonObject?.extractString(vararg keys: String): String? {
    val source = this ?: return null
    return keys.firstNotNullOfOrNull { key ->
        source.get(key)?.takeIf { !it.isJsonNull }?.asString
    }
}

private fun CachedQuestionEntity.toModel(): QuestionSummary {
    return QuestionSummary(
        id = id,
        title = title,
        excerpt = excerpt,
        authorName = authorName,
        authorUsername = authorName,
        authorAvatar = null,
        answerCount = answerCount,
        voteCount = voteCount,
        viewCount = viewCount,
        createdAt = createdAt,
        tags = tags.map { TagItem(name = it) },
        accepted = accepted,
    )
}

private fun CachedQuestionEntity.toDetailFallback(): QuestionDetail {
    return QuestionDetail(
        id = id,
        title = title,
        content = excerpt,
        authorName = authorName,
        authorUsername = authorName,
        authorAvatar = null,
        answerCount = answerCount,
        voteCount = voteCount,
        collectionCount = 0,
        viewCount = viewCount,
        createdAt = createdAt,
        collected = false,
        voteStatus = "",
        tags = tags.map { TagItem(name = it) },
        answers = emptyList(),
        comments = emptyList(),
    )
}
