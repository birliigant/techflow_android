package com.birliigant.techflow.core.model

data class SiteInfo(
    val name: String,
    val description: String,
    val shortDescription: String,
    val logo: String? = null,
)

data class TagItem(
    val id: String = "",
    val name: String,
    val slug: String = name.lowercase(),
)

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String = "",
    val avatar: String? = null,
    val rank: Int = 0,
    val questionCount: Int = 0,
    val answerCount: Int = 0,
    val followCount: Int = 0,
)

data class QuestionSummary(
    val id: String,
    val title: String,
    val excerpt: String,
    val authorName: String,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<TagItem>,
)

data class AnswerItem(
    val id: String,
    val content: String,
    val authorName: String,
    val voteCount: Int,
    val createdAt: String,
    val accepted: Boolean = false,
)

data class CommentItem(
    val id: String,
    val content: String,
    val authorName: String,
    val createdAt: String,
    val replyUsername: String? = null,
)

data class QuestionDetail(
    val id: String,
    val title: String,
    val content: String,
    val authorName: String,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<TagItem>,
    val answers: List<AnswerItem>,
    val comments: List<CommentItem>,
)

data class QuestionDraft(
    val title: String,
    val content: String,
    val tags: List<String>,
    val partition: String,
)

object AppDefaults {
    const val defaultBaseUrl = "http://10.0.2.2:9080/"
}

fun normalizeBaseUrl(raw: String): String {
    val trimmed = raw.trim()
    val withScheme = when {
        trimmed.isBlank() -> AppDefaults.defaultBaseUrl
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        else -> "https://$trimmed"
    }
    return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
}

fun markdownPreview(text: String, maxLength: Int = 180): String {
    val plain = text
        .replace(Regex("`{1,3}"), "")
        .replace(Regex("[#>*_\\-]{1,3}"), " ")
        .replace(Regex("\\[(.*?)]\\((.*?)\\)"), "$1")
        .replace(Regex("\\s+"), " ")
        .trim()

    return if (plain.length <= maxLength) plain else plain.take(maxLength).trimEnd() + "..."
}
