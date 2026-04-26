package com.birliigant.techflow.ui.detail

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.CommentItem
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.core.model.isUpVoted
import com.birliigant.techflow.core.model.shareUrl
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
    val answerComments: Map<String, List<CommentItem>> = emptyMap(),
    val actionMessage: String? = null,
    val errorMessage: String? = null,
)

private enum class AnswerSort(val label: String) {
    SCORE("评分"),
    NEWEST("最新"),
    OLDEST("最旧"),
}

private data class ReportTarget(
    val objectId: String,
    val label: String,
)

class QuestionDetailViewModel(
    private val questionId: String,
    private val questionRepository: QuestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            val result = questionRepository.getQuestionDetail(questionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    detail = result.getOrNull() ?: it.detail,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun dismissActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun toggleQuestionVote() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val result = questionRepository.toggleVoteUp(
                objectId = detail.id,
                cancel = detail.voteStatus.isUpVoted(),
            )
            if (result.isSuccess) {
                refresh(silent = true)
                _uiState.update { it.copy(actionMessage = if (detail.voteStatus.isUpVoted()) "已取消点赞" else "点赞成功") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun toggleQuestionCollection() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val result = questionRepository.toggleCollection(
                objectId = detail.id,
                collected = detail.collected,
            )
            if (result.isSuccess) {
                refresh(silent = true)
                _uiState.update { it.copy(actionMessage = if (detail.collected) "已取消收藏" else "已加入收藏") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun toggleAnswerVote(answer: AnswerItem) {
        viewModelScope.launch {
            val result = questionRepository.toggleVoteUp(
                objectId = answer.id,
                cancel = answer.voteStatus.isUpVoted(),
            )
            if (result.isSuccess) {
                refresh(silent = true)
                _uiState.update { it.copy(actionMessage = if (answer.voteStatus.isUpVoted()) "已取消回答点赞" else "已点赞回答") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun loadAnswerComments(answerId: String) {
        if (_uiState.value.answerComments.containsKey(answerId)) return
        viewModelScope.launch {
            val result = questionRepository.getCommentsForObject(answerId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(answerComments = state.answerComments + (answerId to result.getOrDefault(emptyList())))
                }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun submitQuestionComment(content: String) {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val result = questionRepository.addComment(objectId = detail.id, content = content)
            if (result.isSuccess) {
                refresh(silent = true)
                _uiState.update { it.copy(actionMessage = "评论已发布") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun submitAnswerComment(answerId: String, content: String) {
        viewModelScope.launch {
            val result = questionRepository.addComment(objectId = answerId, content = content)
            if (result.isSuccess) {
                val comments = questionRepository.getCommentsForObject(answerId).getOrDefault(emptyList())
                _uiState.update { state ->
                    state.copy(
                        answerComments = state.answerComments + (answerId to comments),
                        actionMessage = "楼中楼评论已发布",
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun submitReport(objectId: String, content: String, label: String) {
        viewModelScope.launch {
            val result = questionRepository.reportObject(objectId = objectId, content = content)
            if (result.isSuccess) {
                _uiState.update { it.copy(actionMessage = "已提交${label}举报") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun submitAnswer(content: String) {
        viewModelScope.launch {
            val result = questionRepository.createAnswer(questionId = questionId, content = content)
            if (result.isSuccess) {
                refresh(silent = true)
                _uiState.update { it.copy(actionMessage = "回答已发布") }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
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
    val context = LocalContext.current
    var answerSort by remember { mutableStateOf(AnswerSort.SCORE) }
    var questionCommentDialog by remember { mutableStateOf(false) }
    var answerCommentTarget by remember { mutableStateOf<AnswerItem?>(null) }
    var answerComposerDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }

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
            uiState.actionMessage?.let { message ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.dismissActionMessage() },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            val detail = uiState.detail
            if (detail != null) {
                item {
                    DetailHeader(
                        detail = detail,
                        onAuthorClick = { onOpenUserProfile(detail.authorUsername) },
                        onVoteClick = viewModel::toggleQuestionVote,
                        onCollectionClick = viewModel::toggleQuestionCollection,
                        onCommentClick = { questionCommentDialog = true },
                        onShareClick = {
                            shareText(
                                context = context,
                                title = detail.title,
                                body = "${detail.title}\n${detail.shareUrl()}",
                            )
                        },
                        onReportClick = {
                            reportTarget = ReportTarget(detail.id, "帖子")
                        },
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
                            comments = uiState.answerComments[answer.id].orEmpty(),
                            onAuthorClick = { onOpenUserProfile(answer.authorUsername) },
                            onVoteClick = { viewModel.toggleAnswerVote(answer) },
                            onCommentClick = {
                                viewModel.loadAnswerComments(answer.id)
                                answerCommentTarget = answer
                            },
                            onShareClick = {
                                shareText(
                                    context = context,
                                    title = detail.title,
                                    body = "${detail.title}\n${detail.shareUrl()}#answer-${answer.id}\n回答者：${answer.authorName}",
                                )
                            },
                            onReportClick = {
                                reportTarget = ReportTarget(answer.id, "回答")
                            },
                            onLoadComments = { viewModel.loadAnswerComments(answer.id) },
                            onOpenUserProfile = onOpenUserProfile,
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
                        onClick = { answerComposerDialog = true },
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

    if (questionCommentDialog) {
        TextComposerDialog(
            title = "评论帖子",
            hint = "写下你的评论",
            onDismiss = { questionCommentDialog = false },
            onSubmit = {
                viewModel.submitQuestionComment(it)
                questionCommentDialog = false
            },
        )
    }

    answerCommentTarget?.let { answer ->
        TextComposerDialog(
            title = "评论回答",
            hint = "写下你的楼中楼评论",
            onDismiss = { answerCommentTarget = null },
            onSubmit = {
                viewModel.submitAnswerComment(answer.id, it)
                answerCommentTarget = null
            },
        )
    }

    if (answerComposerDialog) {
        TextComposerDialog(
            title = "发布回答",
            hint = "写下你的回答内容",
            onDismiss = { answerComposerDialog = false },
            onSubmit = {
                viewModel.submitAnswer(it)
                answerComposerDialog = false
            },
            minLines = 6,
        )
    }

    reportTarget?.let { target ->
        TextComposerDialog(
            title = "举报${target.label}",
            hint = "可选填写举报说明",
            onDismiss = { reportTarget = null },
            onSubmit = {
                viewModel.submitReport(target.objectId, it, target.label)
                reportTarget = null
            },
        )
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
    onVoteClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
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
                        text = "@${detail.authorUsername} · ${formatDisplayDate(detail.createdAt).ifBlank { "刚刚" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            MarkdownText(detail.content)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${detail.voteCount} 点赞", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${detail.collectionCount} 收藏", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${detail.answerCount} 回答", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${detail.viewCount} 浏览", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InteractionBar(
                voted = detail.voteStatus.isUpVoted(),
                voteCount = detail.voteCount,
                collected = detail.collected,
                collectionCount = detail.collectionCount,
                onVoteClick = onVoteClick,
                onCollectionClick = onCollectionClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onReportClick = onReportClick,
            )
        }
    }
}

@Composable
private fun AnswerCard(
    answer: AnswerItem,
    comments: List<CommentItem>,
    onAuthorClick: () -> Unit,
    onVoteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onLoadComments: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
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
            InteractionBar(
                voted = answer.voteStatus.isUpVoted(),
                voteCount = answer.voteCount,
                onVoteClick = onVoteClick,
                onCommentClick = {
                    onLoadComments()
                    onCommentClick()
                },
                onShareClick = onShareClick,
                onReportClick = onReportClick,
            )
            if (comments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "楼中楼评论",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    comments.forEach { comment ->
                        InlineCommentCard(
                            comment = comment,
                            onAuthorClick = { onOpenUserProfile(comment.authorUsername) },
                        )
                    }
                }
            }
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
                Text(
                    text = "回答于 ${formatDisplayDate(answer.createdAt).ifBlank { "刚刚" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun InteractionBar(
    voted: Boolean,
    voteCount: Int,
    collected: Boolean = false,
    collectionCount: Int? = null,
    onVoteClick: () -> Unit,
    onCollectionClick: (() -> Unit)? = null,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SegmentedActionButton(
            text = if (voted) "已赞 $voteCount" else "点赞 $voteCount",
            selected = voted,
            onClick = onVoteClick,
        )
        if (onCollectionClick != null) {
            SegmentedActionButton(
                text = if (collected) "已收藏 ${collectionCount ?: 0}" else "收藏 ${collectionCount ?: 0}",
                selected = collected,
                onClick = onCollectionClick,
            )
        }
        InlineTextAction(text = "评论", onClick = onCommentClick)
        InlineTextAction(text = "分享", onClick = onShareClick)
        InlineTextAction(text = "举报", onClick = onReportClick)
    }
}

@Composable
private fun SegmentedActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun InlineTextAction(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
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
                text = formatDisplayDate(comment.createdAt).ifBlank { "刚刚" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InlineCommentCard(
    comment: CommentItem,
    onAuthorClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = comment.authorName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = onAuthorClick),
                )
                Text(
                    text = formatDisplayDate(comment.createdAt).ifBlank { "刚刚" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TextComposerDialog(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    minLines: Int = 4,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text.trim()) },
                enabled = text.trim().length >= 2,
            ) {
                Text("提交")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = minLines,
                maxLines = 10,
                placeholder = { Text(hint) },
            )
        },
    )
}

private fun shareText(
    context: android.content.Context,
    title: String,
    body: String,
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "分享到"))
}
