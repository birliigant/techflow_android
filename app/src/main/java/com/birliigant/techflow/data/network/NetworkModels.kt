package com.birliigant.techflow.data.network

import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.BadgeAward
import com.birliigant.techflow.core.model.CommunityUser
import com.birliigant.techflow.core.model.CommentItem
import com.birliigant.techflow.core.model.PersonalCommentActivity
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.core.model.QuestionDraft
import com.birliigant.techflow.core.model.SearchPostItem
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.ReputationActivity
import com.birliigant.techflow.core.model.SiteInfo
import com.birliigant.techflow.core.model.TagDetail
import com.birliigant.techflow.core.model.TagItem
import com.birliigant.techflow.core.model.TagSection
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.core.model.UserProfileUpdate
import com.birliigant.techflow.core.model.VoteActivity
import com.birliigant.techflow.core.model.markdownPreview
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val code: Int? = null,
    val reason: String? = null,
    val msg: String? = null,
    val data: T? = null,
)

data class PageEnvelope<T>(
    val count: Int? = null,
    val list: List<T>? = null,
)

data class SiteInfoDto(
    val name: String? = null,
    val description: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    val logo: String? = null,
)

data class TagDto(
    val id: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("slug_name") val slugName: String? = null,
)

data class TagDetailDto(
    @SerializedName("tag_id") val tagId: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("slug_name") val slugName: String? = null,
    val description: String? = null,
    @SerializedName("follow_count") val followCount: Int? = null,
    @SerializedName("question_count") val questionCount: Int? = null,
    val partition: String? = null,
)

data class TagSectionDto(
    val count: Int? = null,
    val list: List<TagDetailDto>? = null,
    val partition: String? = null,
)

data class SearchItemDto(
    @SerializedName("object_type") val objectType: String? = null,
    @SerializedName("object") val payload: QuestionDto? = null,
)

data class UserDto(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("e_mail") val email: String? = null,
    val avatar: JsonElement? = null,
    val rank: Int? = null,
    @SerializedName("question_count") val questionCount: Int? = null,
    @SerializedName("answer_count") val answerCount: Int? = null,
    @SerializedName("follow_count") val followCount: Int? = null,
    val bio: String? = null,
    val website: String? = null,
    val location: String? = null,
    val profession: String? = null,
)

data class CommunityUserDto(
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val avatar: JsonElement? = null,
    val rank: Int? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
)

data class CommunityUsersDto(
    @SerializedName("users_with_the_most_reputation") val topReputationUsers: List<CommunityUserDto>? = null,
    @SerializedName("users_with_the_most_vote") val topVoteUsers: List<CommunityUserDto>? = null,
    val staffs: List<CommunityUserDto>? = null,
)

data class PublicUserProfileDto(
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val avatar: JsonElement? = null,
    val rank: Int? = null,
    @SerializedName("answer_count") val answerCount: Int? = null,
    @SerializedName("question_count") val questionCount: Int? = null,
    @SerializedName("follow_count") val followCount: Int? = null,
    val bio: String? = null,
    val website: String? = null,
    val location: String? = null,
    val profession: String? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
    @SerializedName(value = "last_login_date", alternate = ["last_login_at"]) val lastLoginAt: String? = null,
)

data class QuestionDto(
    val id: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val title: String? = null,
    @SerializedName("url_title") val urlTitle: String? = null,
    val content: String? = null,
    val html: String? = null,
    @SerializedName("parsed_text") val parsedText: String? = null,
    val excerpt: String? = null,
    @SerializedName("view_count") val viewCount: Int? = null,
    @SerializedName("answer_count") val answerCount: Int? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName("collection_count") val collectionCount: Int? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
    val accepted: JsonElement? = null,
    @SerializedName("accepted_answer_id") val acceptedAnswerId: String? = null,
    val collected: Boolean? = null,
    @SerializedName("vote_status") val voteStatus: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("user_info") val userInfo: UserDto? = null,
    val author: UserDto? = null,
    val operator: UserDto? = null,
    val tags: List<TagDto>? = null,
)

data class AnswerDto(
    val id: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val content: String? = null,
    val html: String? = null,
    @SerializedName("parsed_text") val parsedText: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("user_info") val userInfo: UserDto? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
    val accepted: JsonElement? = null,
    @SerializedName("vote_status") val voteStatus: String? = null,
)

data class CommentDto(
    @SerializedName("comment_id") val commentId: String? = null,
    @SerializedName("original_text") val originalText: String? = null,
    @SerializedName("parsed_text") val parsedText: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("reply_username") val replyUsername: String? = null,
    @SerializedName("user_avatar") val userAvatar: String? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
)

data class PersonalCommentDto(
    @SerializedName("comment_id") val commentId: String? = null,
    val content: String? = null,
    @SerializedName("object_type") val objectType: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val title: String? = null,
    @SerializedName("object_id") val objectId: String? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
)

data class PersonalRankDto(
    @SerializedName("answer_id") val answerId: String? = null,
    @SerializedName("object_id") val objectId: String? = null,
    @SerializedName("object_type") val objectType: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val title: String? = null,
    val content: String? = null,
    @SerializedName("rank_type") val rankType: String? = null,
    val reputation: Int? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
)

data class PersonalVoteDto(
    @SerializedName("answer_id") val answerId: String? = null,
    @SerializedName("object_id") val objectId: String? = null,
    @SerializedName("object_type") val objectType: String? = null,
    @SerializedName("question_id") val questionId: String? = null,
    val title: String? = null,
    val content: String? = null,
    @SerializedName("vote_type") val voteType: String? = null,
    @SerializedName(value = "created_at", alternate = ["create_time"]) val createdAt: String? = null,
)

data class BadgeAwardDto(
    val id: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val level: String? = null,
    @SerializedName("earned_count") val earnedCount: Int? = null,
)

data class EmailLoginRequest(
    @SerializedName("e_mail") val email: String,
    @SerializedName("pass") val password: String,
    @SerializedName("captcha_id") val captchaId: String? = null,
    @SerializedName("captcha_code") val captchaCode: String? = null,
)

data class EmailRegisterRequest(
    val name: String,
    @SerializedName("e_mail") val email: String,
    @SerializedName("pass") val password: String,
    val profession: String? = null,
)

data class CreateQuestionTagRequest(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("original_text") val originalText: String,
    @SerializedName("slug_name") val slugName: String,
)

data class CreateQuestionRequest(
    val title: String,
    val content: String,
    val tags: List<CreateQuestionTagRequest>,
    val partition: String? = null,
)

data class UpdateUserInfoRequest(
    @SerializedName("display_name") val displayName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val website: String? = null,
)

data class ChangeProfessionRequest(
    val profession: String? = null,
)

data class VoteRequest(
    @SerializedName("object_id") val objectId: String,
    @SerializedName("is_cancel") val isCancel: Boolean? = null,
)

data class VoteResponse(
    val votes: Int? = null,
    @SerializedName("up_votes") val upVotes: Int? = null,
    @SerializedName("vote_status") val voteStatus: String? = null,
)

data class CollectionSwitchRequest(
    @SerializedName("object_id") val objectId: String,
    @SerializedName("group_id") val groupId: String = "0",
    val bookmark: Boolean = true,
)

data class CollectionSwitchResponse(
    @SerializedName("object_collection_count") val objectCollectionCount: Int? = null,
)

data class AddCommentRequest(
    @SerializedName("object_id") val objectId: String,
    @SerializedName("original_text") val originalText: String,
    @SerializedName("reply_comment_id") val replyCommentId: String? = null,
    @SerializedName("mention_username_list") val mentionUsernameList: List<String>? = null,
)

data class AddReportRequest(
    @SerializedName("object_id") val objectId: String,
    @SerializedName("report_type") val reportType: Int = 1,
    val content: String? = null,
)

data class CreateAnswerRequest(
    @SerializedName("question_id") val questionId: String,
    val content: String,
)

fun SiteInfoDto.toModel(): SiteInfo {
    return SiteInfo(
        name = name.orEmpty().ifBlank { "TechFlow" },
        description = description.orEmpty(),
        shortDescription = shortDescription.orEmpty(),
        logo = logo,
    )
}

fun TagSectionDto.toModel(): TagSection {
    return TagSection(
        title = partition.orEmpty().ifBlank { "未分组" },
        tags = list.orEmpty().map { it.toModel() },
    )
}

fun TagDetailDto.toModel(): TagDetail {
    val resolvedName = displayName.orEmpty().ifBlank { slugName.orEmpty() }
    return TagDetail(
        id = tagId.orEmpty(),
        name = resolvedName,
        slug = slugName.orEmpty().ifBlank { resolvedName.lowercase() },
        description = description.orEmpty(),
        followCount = followCount ?: 0,
        questionCount = questionCount ?: 0,
        partition = partition.orEmpty(),
    )
}

fun SearchItemDto.toQuestionSummaryOrNull(): QuestionSummary? {
    if (objectType != "question") return null
    return payload?.toSummary()
}

fun SearchItemDto.toSearchPostOrNull(): SearchPostItem? {
    val source = payload ?: return null
    val questionId = source.questionId.orEmpty().ifBlank { source.id.orEmpty() }
    return SearchPostItem(
        objectType = objectType.orEmpty().ifBlank { "question" },
        id = source.id.orEmpty().ifBlank { questionId },
        questionId = questionId,
        title = source.title.orEmpty(),
        excerpt = source.excerpt.orEmpty().ifBlank {
            markdownPreview(
                source.content.orEmpty()
                    .ifBlank { source.parsedText.orEmpty() }
                    .ifBlank { source.html.orEmpty() },
            )
        },
        authorName = source.resolveAuthorName(),
        authorUsername = source.resolveAuthorUsername(),
        authorAvatar = source.resolveAuthorAvatar(),
        answerCount = source.answerCount ?: 0,
        voteCount = source.voteCount ?: 0,
        viewCount = source.viewCount ?: 0,
        createdAt = source.createdAt.orEmpty(),
        tags = source.tags.orEmpty().map { it.toModel() },
        accepted = source.hasAcceptedAnswer(),
    )
}

fun UserDto.toModel(): UserProfile {
    val resolvedUsername = username.orEmpty().ifBlank { "anonymous" }
    return UserProfile(
        id = id.orEmpty(),
        username = resolvedUsername,
        displayName = displayName.orEmpty().ifBlank { resolvedUsername },
        email = email.orEmpty(),
        avatar = avatar.toAvatarUrl(),
        rank = rank ?: 0,
        questionCount = questionCount ?: 0,
        answerCount = answerCount ?: 0,
        followCount = followCount ?: 0,
        bio = bio.orEmpty(),
        website = website.orEmpty(),
        location = location.orEmpty(),
        profession = profession.orEmpty(),
    )
}

fun CommunityUserDto.toModel(): CommunityUser {
    val resolvedUsername = username.orEmpty()
    return CommunityUser(
        username = resolvedUsername,
        displayName = displayName.orEmpty().ifBlank { resolvedUsername },
        avatar = avatar.toAvatarUrl(),
        rank = rank ?: 0,
        voteCount = voteCount ?: 0,
    )
}

fun PublicUserProfileDto.toModel(): PublicUserProfile {
    val resolvedUsername = username.orEmpty()
    return PublicUserProfile(
        username = resolvedUsername,
        displayName = displayName.orEmpty().ifBlank { resolvedUsername },
        avatar = avatar.toAvatarUrl(),
        rank = rank ?: 0,
        answerCount = answerCount ?: 0,
        questionCount = questionCount ?: 0,
        followCount = followCount ?: 0,
        bio = bio.orEmpty(),
        website = website.orEmpty(),
        location = location.orEmpty(),
        profession = profession.orEmpty(),
        createdAt = createdAt.orEmpty(),
        lastLoginAt = lastLoginAt.orEmpty(),
    )
}

fun JsonElement?.toPublicUserProfile(): PublicUserProfile? {
    val root = this?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    val source = root.get("info")?.takeIf { it.isJsonObject }?.asJsonObject ?: root
    val resolvedUsername = source.stringValue("username").orEmpty()
    return PublicUserProfile(
        username = resolvedUsername,
        displayName = source.stringValue("display_name").orEmpty().ifBlank { resolvedUsername },
        avatar = source.get("avatar").toAvatarUrl(),
        rank = source.intValue("rank") ?: 0,
        answerCount = source.intValue("answer_count") ?: 0,
        questionCount = source.intValue("question_count") ?: 0,
        followCount = source.intValue("follow_count") ?: 0,
        bio = source.stringValue("bio").orEmpty(),
        website = source.stringValue("website").orEmpty(),
        location = source.stringValue("location").orEmpty(),
        profession = source.stringValue("profession").orEmpty(),
        createdAt = source.stringValue("created_at").orEmpty(),
        lastLoginAt = source.stringValue("last_login_date").orEmpty(),
    )
}

fun JsonElement?.toBadgeAwardList(): List<BadgeAward> {
    val items = when {
        this == null || isJsonNull -> emptyList()
        isJsonArray -> asJsonArray.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
        isJsonObject -> asJsonObject.get("list")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
            .orEmpty()
        else -> emptyList()
    }
    return items.map { it.toBadgeAwardModel() }
}

fun QuestionDto.toSummary(): QuestionSummary {
    val contentText = content.orEmpty().ifBlank { parsedText.orEmpty() }.ifBlank { html.orEmpty() }
    return QuestionSummary(
        id = id.orEmpty(),
        title = title.orEmpty(),
        excerpt = excerpt.orEmpty().ifBlank { markdownPreview(contentText) },
        authorName = resolveAuthorName(),
        authorUsername = resolveAuthorUsername(),
        authorAvatar = resolveAuthorAvatar(),
        answerCount = answerCount ?: 0,
        voteCount = voteCount ?: 0,
        viewCount = viewCount ?: 0,
        createdAt = createdAt.orEmpty(),
        tags = tags.orEmpty().map { it.toModel() },
        accepted = hasAcceptedAnswer(),
    )
}

fun QuestionDto.toDetail(
    answers: List<AnswerItem>,
    comments: List<CommentItem>,
): QuestionDetail {
    return QuestionDetail(
        id = id.orEmpty(),
        title = title.orEmpty(),
        content = content.orEmpty().ifBlank { parsedText.orEmpty() }.ifBlank { html.orEmpty() },
        urlTitle = urlTitle.orEmpty(),
        authorName = resolveAuthorName(),
        authorUsername = resolveAuthorUsername(),
        authorAvatar = resolveAuthorAvatar(),
        answerCount = answerCount ?: answers.size,
        voteCount = voteCount ?: 0,
        collectionCount = collectionCount ?: 0,
        viewCount = viewCount ?: 0,
        createdAt = createdAt.orEmpty(),
        collected = collected == true,
        voteStatus = voteStatus.orEmpty(),
        tags = tags.orEmpty().map { it.toModel() },
        answers = answers,
        comments = comments,
    )
}

fun AnswerDto.toModel(): AnswerItem {
    return AnswerItem(
        id = id.orEmpty(),
        content = content.orEmpty().ifBlank { parsedText.orEmpty() }.ifBlank { html.orEmpty() },
        authorName = userDisplayName.orEmpty()
            .ifBlank { userInfo?.displayName.orEmpty() }
            .ifBlank { username.orEmpty() }
            .ifBlank { "匿名回答者" },
        authorUsername = username.orEmpty()
            .ifBlank { userInfo?.username.orEmpty() }
            .ifBlank { userDisplayName.orEmpty() },
        authorAvatar = userInfo?.avatar.toAvatarUrl(),
        voteCount = voteCount ?: 0,
        createdAt = createdAt.orEmpty(),
        accepted = accepted.toBooleanCompat(),
        voteStatus = voteStatus.orEmpty(),
    )
}

fun CommentDto.toModel(): CommentItem {
    return CommentItem(
        id = commentId.orEmpty(),
        content = originalText.orEmpty().ifBlank { parsedText.orEmpty() },
        authorName = userDisplayName.orEmpty().ifBlank { username.orEmpty() }.ifBlank { "访客" },
        authorUsername = username.orEmpty().ifBlank { userDisplayName.orEmpty() },
        createdAt = createdAt.orEmpty(),
        replyUsername = replyUsername,
    )
}

fun PersonalCommentDto.toModel(): PersonalCommentActivity {
    return PersonalCommentActivity(
        id = commentId.orEmpty().ifBlank { objectId.orEmpty() },
        objectType = objectType.orEmpty(),
        questionId = questionId.orEmpty().ifBlank { objectId.orEmpty() },
        title = title.orEmpty(),
        content = content.orEmpty(),
        createdAt = createdAt.orEmpty(),
    )
}

fun PersonalRankDto.toModel(): ReputationActivity {
    return ReputationActivity(
        id = answerId.orEmpty().ifBlank { objectId.orEmpty() },
        objectType = objectType.orEmpty(),
        questionId = questionId.orEmpty().ifBlank { objectId.orEmpty() },
        title = title.orEmpty(),
        content = content.orEmpty(),
        rankType = rankType.orEmpty(),
        reputation = reputation ?: 0,
        createdAt = createdAt.orEmpty(),
    )
}

fun PersonalVoteDto.toModel(): VoteActivity {
    return VoteActivity(
        id = answerId.orEmpty().ifBlank { objectId.orEmpty() },
        objectType = objectType.orEmpty(),
        questionId = questionId.orEmpty().ifBlank { objectId.orEmpty() },
        title = title.orEmpty(),
        content = content.orEmpty(),
        voteType = voteType.orEmpty(),
        createdAt = createdAt.orEmpty(),
    )
}

fun BadgeAwardDto.toModel(): BadgeAward {
    return BadgeAward(
        id = id.orEmpty(),
        name = name.orEmpty(),
        icon = icon?.normalizeRemoteUrl().orEmpty().ifBlank { null },
        level = level.orEmpty(),
        earnedCount = earnedCount ?: 0,
    )
}

fun TagDto.toModel(): TagItem {
    val resolvedName = displayName.orEmpty().ifBlank { slugName.orEmpty() }
    return TagItem(
        id = id.orEmpty(),
        name = resolvedName,
        slug = slugName.orEmpty().ifBlank { resolvedName.lowercase() },
    )
}

fun QuestionDraft.toRequest(): CreateQuestionRequest {
    return CreateQuestionRequest(
        title = title,
        content = content,
        tags = tags.map { tag ->
            CreateQuestionTagRequest(
                displayName = tag,
                originalText = tag,
                slugName = tag,
            )
        },
        partition = partition.ifBlank { null },
    )
}

fun UserProfileUpdate.toInfoRequest(): UpdateUserInfoRequest {
    return UpdateUserInfoRequest(
        displayName = displayName,
        username = username,
        bio = bio,
        location = location,
        website = website,
    )
}

fun UserProfileUpdate.toProfessionRequest(): ChangeProfessionRequest {
    return ChangeProfessionRequest(
        profession = profession,
    )
}

private fun QuestionDto.resolveAuthorName(): String {
    return userDisplayName.orEmpty()
        .ifBlank { userInfo?.displayName.orEmpty() }
        .ifBlank { author?.displayName.orEmpty() }
        .ifBlank { operator?.displayName.orEmpty() }
        .ifBlank { username.orEmpty() }
        .ifBlank { userInfo?.username.orEmpty() }
        .ifBlank { author?.username.orEmpty() }
        .ifBlank { operator?.username.orEmpty() }
        .ifBlank { "匿名作者" }
}

private fun QuestionDto.resolveAuthorUsername(): String {
    return username.orEmpty()
        .ifBlank { userInfo?.username.orEmpty() }
        .ifBlank { author?.username.orEmpty() }
        .ifBlank { operator?.username.orEmpty() }
        .ifBlank { resolveAuthorName() }
}

private fun QuestionDto.resolveAuthorAvatar(): String? {
    return userInfo?.avatar.toAvatarUrl()
        ?: author?.avatar.toAvatarUrl()
        ?: operator?.avatar.toAvatarUrl()
}

private fun JsonElement?.toBooleanCompat(): Boolean {
    val value = this ?: return false
    if (value.isJsonPrimitive) {
        val primitive = value.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asInt != 0
            primitive.isString -> primitive.asString == "1" || primitive.asString.equals("true", ignoreCase = true)
            else -> false
        }
    }
    return false
}

private fun QuestionDto.hasAcceptedAnswer(): Boolean {
    if (accepted.toBooleanCompat()) return true
    val acceptedId = acceptedAnswerId.orEmpty().trim()
    return acceptedId.isNotBlank() && acceptedId != "0"
}

private fun JsonObject.toBadgeAwardModel(): BadgeAward {
    return BadgeAward(
        id = stringValue("id").orEmpty(),
        name = stringValue("name").orEmpty(),
        icon = get("icon").toAvatarUrl(),
        level = stringValue("level").orEmpty(),
        earnedCount = intValue("earned_count") ?: 0,
    )
}

private fun JsonObject.stringValue(key: String): String? {
    val value = get(key) ?: return null
    if (value.isJsonNull) return null
    return value.asString
}

private fun JsonObject.intValue(key: String): Int? {
    val value = get(key) ?: return null
    if (value.isJsonNull) return null
    return runCatching { value.asInt }.getOrNull()
}

private fun JsonElement?.toAvatarUrl(): String? {
    val value = this ?: return null
    if (value.isJsonNull) return null
    if (value.isJsonPrimitive) {
        return value.asString.takeIf { it.isNotBlank() }?.normalizeRemoteUrl()
    }
    if (!value.isJsonObject) return null

    val obj = value.asJsonObject
    val directKeys = listOf("avatar", "url", "src", "original", "small", "large")
    directKeys.forEach { key ->
        obj.get(key)?.let { nested ->
            nested.toAvatarUrl()?.let { return it }
        }
    }

    val nestedKeys = listOf("upload_file", "image", "urls")
    nestedKeys.forEach { key ->
        obj.get(key)?.let { nested ->
            nested.toAvatarUrl()?.let { return it }
        }
    }

    return null
}

fun String.normalizeRemoteUrl(): String {
    val raw = trim()
    return when {
        raw.isBlank() -> raw
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("/") -> "${com.birliigant.techflow.core.model.AppDefaults.defaultBaseUrl.removeSuffix("/")}$raw"
        else -> raw
    }
}
