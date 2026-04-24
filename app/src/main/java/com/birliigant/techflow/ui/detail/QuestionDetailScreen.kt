package com.birliigant.techflow.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.QuestionDetail
import com.birliigant.techflow.data.repository.QuestionRepository
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    DetailHeader(detail)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionSwitch(text = "评分", selected = true, onClick = {})
                        SectionSwitch(text = "最新", selected = false, onClick = {})
                        SectionSwitch(text = "最旧", selected = false, onClick = {})
                    }
                }

                item {
                    Text("回答", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                if (detail.answers.isEmpty()) {
                    item {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("还没有回答。", modifier = Modifier.padding(20.dp))
                        }
                    }
                } else {
                    items(detail.answers, key = { it.id }) { answer ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(answer.content)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("${answer.voteCount} 赞", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(answer.authorName, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                if (detail.comments.isNotEmpty()) {
                    item {
                        Text("评论", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    items(detail.comments, key = { it.id }) { comment ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(comment.content)
                            Text(
                                text = buildString {
                                    append(comment.authorName)
                                    comment.replyUsername?.takeIf { it.isNotBlank() }?.let { append(" 回复 $it") }
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        }
                    }
                }

                item {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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

@Composable
private fun DetailHeader(detail: QuestionDetail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${detail.authorName} · ${detail.createdAt.ifBlank { "刚刚" }}",
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = detail.content.ifBlank { "暂无正文" },
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${detail.voteCount} 点赞", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${detail.answerCount} 回答", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${detail.viewCount} 浏览", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
