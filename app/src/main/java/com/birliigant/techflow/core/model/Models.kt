package com.birliigant.techflow.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

data class TagSection(
    val title: String,
    val tags: List<TagDetail>,
)

data class TagDetail(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val followCount: Int,
    val questionCount: Int,
    val partition: String,
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
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val profession: String = "",
)

data class CommunityUser(
    val username: String,
    val displayName: String,
    val avatar: String? = null,
    val rank: Int = 0,
    val voteCount: Int = 0,
)

data class PublicUserProfile(
    val username: String,
    val displayName: String,
    val avatar: String? = null,
    val rank: Int = 0,
    val answerCount: Int = 0,
    val questionCount: Int = 0,
    val followCount: Int = 0,
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val profession: String = "",
    val createdAt: String = "",
    val lastLoginAt: String = "",
)

data class ReputationActivity(
    val id: String,
    val objectType: String,
    val questionId: String,
    val title: String,
    val content: String,
    val rankType: String,
    val reputation: Int,
    val createdAt: String,
)

data class PersonalCommentActivity(
    val id: String,
    val objectType: String,
    val questionId: String,
    val title: String,
    val content: String,
    val createdAt: String,
)

data class VoteActivity(
    val id: String,
    val objectType: String,
    val questionId: String,
    val title: String,
    val content: String,
    val voteType: String,
    val createdAt: String,
)

data class BadgeAward(
    val id: String,
    val name: String,
    val icon: String? = null,
    val level: String = "",
    val earnedCount: Int = 0,
)

data class QuestionSummary(
    val id: String,
    val title: String,
    val excerpt: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<TagItem>,
    val accepted: Boolean = false,
)

data class SearchPostItem(
    val objectType: String,
    val id: String,
    val questionId: String,
    val title: String,
    val excerpt: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<TagItem>,
    val accepted: Boolean = false,
)

data class AnswerItem(
    val id: String,
    val content: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val voteCount: Int,
    val createdAt: String,
    val accepted: Boolean = false,
)

data class CommentItem(
    val id: String,
    val content: String,
    val authorName: String,
    val authorUsername: String,
    val createdAt: String,
    val replyUsername: String? = null,
)

data class QuestionDetail(
    val id: String,
    val title: String,
    val content: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<TagItem>,
    val answers: List<AnswerItem>,
    val comments: List<CommentItem>,
)

data class UserProfileUpdate(
    val displayName: String,
    val username: String,
    val bio: String,
    val location: String,
    val website: String,
    val profession: String,
)

data class QuestionDraft(
    val title: String,
    val content: String,
    val tags: List<String>,
    val partition: String,
)

object AppDefaults {
    const val defaultBaseUrl = "https://answer.sipc115.com/"
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

fun formatDisplayDate(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""

    val directDate = Regex("""\d{4}-\d{2}-\d{2}""").find(trimmed)?.value
    if (directDate != null) return directDate

    val epoch = trimmed.toLongOrNull()
    if (epoch != null) {
        val epochMillis = if (trimmed.length <= 10) epoch * 1000 else epoch
        return runCatching {
            Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrDefault(trimmed)
    }

    return trimmed
}

fun reputationTypeLabel(raw: String): String {
    return when (raw.lowercase()) {
        "question" -> "问题"
        "answer" -> "回答"
        "comment" -> "评论"
        "accepted" -> "回答被采纳"
        "vote_up" -> "获赞"
        "vote_down" -> "被点踩"
        else -> raw.ifBlank { "声望变动" }
    }
}

fun voteTypeLabel(raw: String): String {
    return when (raw.lowercase()) {
        "upvote", "up", "vote_up" -> "点赞"
        "downvote", "down", "vote_down" -> "点踩"
        else -> raw.ifBlank { "投票" }
    }
}

fun objectTypeLabel(raw: String): String {
    return when (raw.lowercase()) {
        "question" -> "问题"
        "answer" -> "回答"
        "comment" -> "评论"
        "tag" -> "标签"
        else -> raw.ifBlank { "内容" }
    }
}

fun badgeLevelLabel(raw: String): String {
    return when (raw.lowercase()) {
        "gold" -> "金牌"
        "silver" -> "银牌"
        "bronze" -> "铜牌"
        else -> raw.ifBlank { "徽章" }
    }
}
