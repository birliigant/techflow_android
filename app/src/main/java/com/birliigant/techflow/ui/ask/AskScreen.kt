package com.birliigant.techflow.ui.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.QuestionDraft
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskUiState(
    val title: String = "",
    val content: String = "",
    val tagsInput: String = "",
    val partition: String = "general",
    val isLoggedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
)

class AskViewModel(
    private val questionRepository: QuestionRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val editorState = MutableStateFlow(AskUiState())
    private val submitEvents = MutableSharedFlow<Unit>()

    val uiState: StateFlow<AskUiState> = combine(
        editorState,
        sessionRepository.token,
    ) { state, token ->
        state.copy(isLoggedIn = token.isNotBlank())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = editorState.value,
    )

    val successEvents = submitEvents.asSharedFlow()

    fun updateTitle(value: String) = editorState.update { it.copy(title = value) }

    fun updateContent(value: String) = editorState.update { it.copy(content = value) }

    fun updateTags(value: String) = editorState.update { it.copy(tagsInput = value) }

    fun updatePartition(value: String) = editorState.update { it.copy(partition = value) }

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun submitQuestion() {
        val state = uiState.value
        if (!state.isLoggedIn) {
            editorState.update { it.copy(message = "请先在“我的”页面配置 token 或登录。") }
            return
        }
        if (state.title.length < 6 || state.content.length < 6) {
            editorState.update { it.copy(message = "标题和正文至少需要 6 个字符。") }
            return
        }

        viewModelScope.launch {
            editorState.update { it.copy(isSubmitting = true, message = null) }
            val draft = QuestionDraft(
                title = state.title.trim(),
                content = state.content.trim(),
                tags = state.tagsInput.split(",", "，").map { it.trim() }.filter { it.isNotBlank() },
                partition = state.partition.trim().ifBlank { "general" },
            )
            val result = questionRepository.createQuestion(draft)
            if (result.isSuccess) {
                editorState.value = AskUiState(message = "问题已提交。")
                submitEvents.emit(Unit)
            } else {
                editorState.update {
                    it.copy(
                        isSubmitting = false,
                        message = result.exceptionOrNull()?.message ?: "提交失败",
                    )
                }
            }
        }
    }
}

@Composable
fun AskScreen(
    viewModel: AskViewModel,
    onGoProfile: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.successEvents.collect {
            onSubmitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("发布问题", style = MaterialTheme.typography.headlineSmall)
                        Text("根据接口文档，发帖至少需要标题、正文、标签和分区。")
                        if (!uiState.isLoggedIn) {
                            Button(onClick = onGoProfile) {
                                Text("先去登录")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    minLines = 2,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::updateContent,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("正文（Markdown）") },
                    minLines = 8,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.tagsInput,
                    onValueChange = viewModel::updateTags,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标签，逗号分隔") },
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.partition,
                    onValueChange = viewModel::updatePartition,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("分区") },
                )
            }
            item {
                Button(
                    onClick = viewModel::submitQuestion,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSubmitting) "提交中..." else "发布问题")
                }
            }
        }
    }
}
