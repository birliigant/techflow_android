package com.birliigant.techflow.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.CommentItem
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.MarkdownText
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionDetailUiState(
    val isLoading: Boolean = true,
    val detail: QuestionDetail? = null,
    val errorMessage: String? = null,
)

private enum class AnswerSort(val label: String) {
    SCORE("评分"),
    NEWEST("最新"),
    OLDEST("最旧"),
}

class QuestionDetailViewModel(
    private val questionId: String,
    private val questionRepository: QuestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = questionRepository.getQuestionDetail(questionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    detail = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}

@Composable
fun QuestionDetailScreen(
    viewModel: QuestionDetailViewModel,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var answerSort by remember { mutableStateOf(AnswerSort.SCORE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "SIPC TechFlow",
            onBackClick = onBack,
        )

        if (uiState.isLoading && uiState.detail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val detail = uiState.detail
            if (detail != null) {
                item {
                    DetailHeader(
                        detail = detail,
                        onAuthorClick = { onOpenUserProfile(detail.authorUsername) },
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnswerSort.entries.forEach { sort ->
                            SectionSwitch(
                                text = sort.label,
                                selected = answerSort == sort,
                                onClick = { answerSort = sort },
                            )
                        }
                    }
                }

                item {
                    Text("回答", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                if (detail.answers.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text("还没有回答。", modifier = Modifier.padding(20.dp))
                        }
                    }
                } else {
                    items(detail.answers.sortedBy(answerSort), key = { it.id }) { answer ->
                        AnswerCard(
                            answer = answer,
                            onAuthorClick = { onOpenUserProfile(answer.authorUsername) },
                        )
                    }
                }

                if (detail.comments.isNotEmpty()) {
                    item {
                        Text("评论", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    items(detail.comments, key = { it.id }) { comment ->
                        CommentCard(
                            comment = comment,
                            onAuthorClick = { onOpenUserProfile(comment.authorUsername) },
                            onReplyClick = { username -> onOpenUserProfile(username) },
                        )
                    }
                }

                item {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        Text("我要回答")
                    }
                }
            } else {
                item {
                    ElevatedCard {
                        Text(
                            text = uiState.errorMessage ?: "问题详情加载失败。",
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun List<AnswerItem>.sortedBy(sort: AnswerSort): List<AnswerItem> {
    return when (sort) {
        AnswerSort.SCORE -> sortedWith(compareByDescending<AnswerItem> { it.voteCount }.thenByDescending { it.createdAt.toLongOrNull() ?: 0L })
        AnswerSort.NEWEST -> sortedByDescending { it.createdAt.toLongOrNull() ?: 0L }
        AnswerSort.OLDEST -> sortedBy { it.createdAt.toLongOrNull() ?: 0L }
    }
}

@Composable
private fun DetailHeader(
    detail: QuestionDetail,
    onAuthorClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(
                    imageUrl = detail.authorAvatar,
                    fallbackText = detail.authorName,
                    modifier = Modifier.size(44.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = detail.authorName,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onAuthorClick),
                    )
                    Text(
                        text = "@${detail.authorUsername} · ${detail.createdAt.ifBlank { "刚刚" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            MarkdownText(detail.content)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${detail.voteCount} 点赞", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${detail.answerCount} 回答", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${detail.viewCount} 浏览", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AnswerCard(
    answer: AnswerItem,
    onAuthorClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (answer.accepted) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "已采纳",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            MarkdownText(answer.content)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AvatarImage(
                        imageUrl = answer.authorAvatar,
                        fallbackText = answer.authorName,
                        modifier = Modifier.size(38.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = answer.authorName,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onAuthorClick),
                        )
                        Text(
                            text = "@${answer.authorUsername}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${answer.voteCount} 赞", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = answer.createdAt.ifBlank { "刚刚" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: CommentItem,
    onAuthorClick: () -> Unit,
    onReplyClick: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MarkdownText(
                content = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = comment.authorName,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onAuthorClick),
                )
                comment.replyUsername?.takeIf { it.isNotBlank() }?.let { reply ->
                    Text("回复", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "@$reply",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onReplyClick(reply) },
                    )
                }
            }
            Text(
                text = comment.createdAt.ifBlank { "刚刚" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
