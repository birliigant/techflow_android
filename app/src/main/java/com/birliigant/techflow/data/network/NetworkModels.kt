package com.birliigant.techflow.data.network

import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.CommentItem
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.core.model.QuestionDraft
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.SiteInfo
import com.birliigant.techflow.core.model.TagItem
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.core.model.markdownPreview
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

data class UserDto(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("e_mail") val email: String? = null,
    val avatar: String? = null,
    val rank: Int? = null,
    @SerializedName("question_count") val questionCount: Int? = null,
    @SerializedName("answer_count") val answerCount: Int? = null,
    @SerializedName("follow_count") val followCount: Int? = null,
)

data class QuestionDto(
    val id: String? = null,
    val title: String? = null,
    val content: String? = null,
    @SerializedName("parsed_text") val parsedText: String? = null,
    val excerpt: String? = null,
    @SerializedName("view_count") val viewCount: Int? = null,
    @SerializedName("answer_count") val answerCount: Int? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("user_info") val userInfo: UserDto? = null,
    val author: UserDto? = null,
    val tags: List<TagDto>? = null,
)

data class AnswerDto(
    val id: String? = null,
    val content: String? = null,
    @SerializedName("parsed_text") val parsedText: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("user_info") val userInfo: UserDto? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val accepted: Boolean? = null,
)

data class CommentDto(
    @SerializedName("comment_id") val commentId: String? = null,
    @SerializedName("original_text") val originalText: String? = null,
    val username: String? = null,
    @SerializedName("user_display_name") val userDisplayName: String? = null,
    @SerializedName("reply_username") val replyUsername: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class EmailLoginRequest(
    @SerializedName("e_mail") val email: String,
    @SerializedName("pass") val password: String,
    @SerializedName("captcha_id") val captchaId: String? = null,
    @SerializedName("captcha_code") val captchaCode: String? = null,
)

data class CreateQuestionRequest(
    val title: String,
    val content: String,
    val tags: List<String>,
    val partition: String,
)

fun SiteInfoDto.toModel(): SiteInfo {
    return SiteInfo(
        name = name.orEmpty().ifBlank { "TechFlow" },
        description = description.orEmpty(),
        shortDescription = shortDescription.orEmpty(),
        logo = logo,
    )
}

fun UserDto.toModel(): UserProfile {
    val resolvedUsername = username.orEmpty().ifBlank { "anonymous" }
    return UserProfile(
        id = id.orEmpty(),
        username = resolvedUsername,
        displayName = displayName.orEmpty().ifBlank { resolvedUsername },
        email = email.orEmpty(),
        avatar = avatar,
        rank = rank ?: 0,
        questionCount = questionCount ?: 0,
        answerCount = answerCount ?: 0,
        followCount = followCount ?: 0,
    )
}

fun QuestionDto.toSummary(): QuestionSummary {
    val contentText = content.orEmpty().ifBlank { parsedText.orEmpty() }
    return QuestionSummary(
        id = id.orEmpty(),
        title = title.orEmpty(),
        excerpt = excerpt.orEmpty().ifBlank { markdownPreview(contentText) },
        authorName = resolveAuthorName(),
        answerCount = answerCount ?: 0,
        voteCount = voteCount ?: 0,
        viewCount = viewCount ?: 0,
        createdAt = createdAt.orEmpty(),
        tags = tags.orEmpty().map { it.toModel() },
    )
}

fun QuestionDto.toDetail(
    answers: List<AnswerItem>,
    comments: List<CommentItem>,
): QuestionDetail {
    return QuestionDetail(
        id = id.orEmpty(),
        title = title.orEmpty(),
        content = content.orEmpty().ifBlank { parsedText.orEmpty() },
        authorName = resolveAuthorName(),
        answerCount = answerCount ?: answers.size,
        voteCount = voteCount ?: 0,
        viewCount = viewCount ?: 0,
        createdAt = createdAt.orEmpty(),
        tags = tags.orEmpty().map { it.toModel() },
        answers = answers,
        comments = comments,
    )
}

fun AnswerDto.toModel(): AnswerItem {
    return AnswerItem(
        id = id.orEmpty(),
        content = content.orEmpty().ifBlank { parsedText.orEmpty() },
        authorName = userDisplayName.orEmpty()
            .ifBlank { userInfo?.displayName.orEmpty() }
            .ifBlank { username.orEmpty() }
            .ifBlank { "匿名回答者" },
        voteCount = voteCount ?: 0,
        createdAt = createdAt.orEmpty(),
        accepted = accepted ?: false,
    )
}

fun CommentDto.toModel(): CommentItem {
    return CommentItem(
        id = commentId.orEmpty(),
        content = originalText.orEmpty(),
        authorName = userDisplayName.orEmpty().ifBlank { username.orEmpty() }.ifBlank { "访客" },
        createdAt = createdAt.orEmpty(),
        replyUsername = replyUsername,
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
        tags = tags,
        partition = partition,
    )
}

private fun QuestionDto.resolveAuthorName(): String {
    return userDisplayName.orEmpty()
        .ifBlank { userInfo?.displayName.orEmpty() }
        .ifBlank { author?.displayName.orEmpty() }
        .ifBlank { username.orEmpty() }
        .ifBlank { userInfo?.username.orEmpty() }
        .ifBlank { author?.username.orEmpty() }
        .ifBlank { "匿名作者" }
}
