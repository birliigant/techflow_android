package com.birliigant.techflow.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.TagDetail
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagFeedUiState(
    val isLoading: Boolean = true,
    val questions: List<QuestionSummary> = emptyList(),
    val errorMessage: String? = null,
)

class TagFeedViewModel(
    val tag: TagDetail,
    private val questionRepository: QuestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagFeedUiState())
    val uiState: StateFlow<TagFeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = questionRepository.searchQuestions(
                query = tag.name.ifBlank { tag.slug },
                pageSize = 30,
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    questions = result.getOrNull().orEmpty(),
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}

@Composable
fun TagFeedScreen(
    viewModel: TagFeedViewModel,
    onBack: () -> Unit,
    onQuestionClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tag = viewModel.tag

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = tag.name,
            onBackClick = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TagHeroCard(tag = tag)
            }

            if (uiState.isLoading && uiState.questions.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.errorMessage != null && uiState.questions.isEmpty()) {
                item {
                    ElevatedCard {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.questions.isEmpty() && uiState.errorMessage == null) {
                item {
                    ElevatedCard {
                        Text(
                            text = "这个标签下暂时还没有可展示的问题内容。",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(uiState.questions, key = { it.id }) { question ->
                TagQuestionCard(
                    question = question,
                    onClick = { onQuestionClick(question.id) },
                    onUserClick = { onUserClick(question.authorUsername) },
                )
            }
        }
    }
}

@Composable
private fun TagHeroCard(tag: TagDetail) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "#${tag.name}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = tag.description.ifBlank { "浏览与“${tag.name}”相关的问题内容。" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionSwitch(
                    text = "${tag.questionCount} 帖子",
                    selected = true,
                    onClick = {},
                )
                SectionSwitch(
                    text = "${tag.followCount} 关注",
                    selected = false,
                    onClick = {},
                )
            }
            if (tag.partition.isNotBlank()) {
                Text(
                    text = "分区：${tag.partition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TagQuestionCard(
    question: QuestionSummary,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(
                    imageUrl = question.authorAvatar,
                    fallbackText = question.authorName,
                    modifier = Modifier.size(42.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = question.authorName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onUserClick),
                    )
                    Text(
                        text = "@${question.authorUsername} · ${formatDisplayDate(question.createdAt).ifBlank { "刚刚" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (question.excerpt.isNotBlank()) {
                Text(
                    text = question.excerpt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${question.voteCount} 点赞 · ${question.answerCount} 回答 · ${question.viewCount} 浏览",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
