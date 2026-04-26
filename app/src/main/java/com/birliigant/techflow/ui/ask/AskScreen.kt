@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.birliigant.techflow.ui.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.QuestionDraft
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.ui.common.MarkdownText
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowFooter
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class AskPartition(
    val label: String,
    val value: String,
)

private val askPartitions = listOf(
    AskPartition("考研保研", "research"),
    AskPartition("实习就业", "internship"),
    AskPartition("校内作业", "school"),
    AskPartition("代码编程", "code"),
    AskPartition("日常思考", "deepseek"),
    AskPartition("专业相关", "major"),
    AskPartition("竞赛相关", "competition"),
)

data class AskUiState(
    val title: String = "",
    val content: String = "",
    val tagsInput: String = "",
    val partition: String = askPartitions.first().value,
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

    fun resetDraft() {
        editorState.value = AskUiState(isLoggedIn = uiState.value.isLoggedIn)
    }

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun submitQuestion() {
        val state = uiState.value
        if (!state.isLoggedIn) {
            editorState.update { it.copy(message = "请先登录后再发布问题。") }
            return
        }
        if (state.title.trim().length < 6 || state.content.trim().length < 6) {
            editorState.update { it.copy(message = "标题和正文至少需要 6 个字符。") }
            return
        }
        val tags = state.tagsInput.split(",", "，", " ").map { it.trim() }.filter { it.isNotBlank() }
        if (tags.isEmpty()) {
            editorState.update { it.copy(message = "至少需要填写一个标签。") }
            return
        }

        viewModelScope.launch {
            editorState.update { it.copy(isSubmitting = true, message = null) }
            val draft = QuestionDraft(
                title = state.title.trim(),
                content = state.content.trim(),
                tags = tags,
                partition = state.partition,
            )
            val result = questionRepository.createQuestion(draft)
            if (result.isSuccess) {
                editorState.value = AskUiState(message = "问题已提交。", isLoggedIn = true)
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
    var contentValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.content))
    }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.content) {
        if (uiState.content != contentValue.text) {
            contentValue = TextFieldValue(uiState.content, selection = TextRange(uiState.content.length))
        }
    }

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
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "新增问题",
            showMenu = false,
        )
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "你想提问的内容类型为？",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        askPartitions.forEach { partition ->
                            SectionSwitch(
                                text = partition.label,
                                selected = uiState.partition == partition.value,
                                onClick = { viewModel.updatePartition(partition.value) },
                            )
                        }
                    }
                }
            }

            item {
                EditorBlock(
                    title = "给你的问题写一个吸引人的标题吧：",
                    body = {
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::updateTitle,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例：如何科学地上班摸鱼？") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                    },
                )
            }

            item {
                EditorBlock(
                    title = "清晰、详细地描述下你的问题：",
                    body = {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            MarkdownToolbar(
                                onInsert = { operation ->
                                    contentValue = operation(contentValue)
                                    viewModel.updateContent(contentValue.text)
                                },
                                onOpenHelp = { showHelpDialog = true },
                            )
                            OutlinedTextField(
                                value = contentValue,
                                onValueChange = {
                                    contentValue = it
                                    viewModel.updateContent(it.text)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 10,
                                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    },
                )
            }

            item {
                EditorBlock(
                    title = "为问题补充标签：",
                    body = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.tagsInput,
                                onValueChange = viewModel::updateTags,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("多个标签用逗号分隔，例如：后端, Java, Spring") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                            )
                            if (uiState.tagsInput.isNotBlank()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    uiState.tagsInput
                                        .split(",", "，", " ")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                        .forEach { tag ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(999.dp),
                                            ) {
                                                Text(
                                                    text = tag,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    },
                )
            }

            item {
                EditorBlock(
                    title = "实时预览：",
                    body = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (uiState.content.isBlank()) {
                                Text(
                                    text = "开始输入正文后，这里会实时展示 Markdown 预览。",
                                    modifier = Modifier.padding(18.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    MarkdownText(uiState.content)
                                }
                            }
                        }
                    },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = viewModel::submitQuestion,
                        enabled = !uiState.isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (uiState.isSubmitting) "提交中..." else "提交问题")
                    }
                    TextButton(onClick = viewModel::resetDraft) {
                        Text("丢弃草稿")
                    }
                }
            }

            if (!uiState.isLoggedIn) {
                item {
                    TextButton(onClick = onGoProfile) {
                        Text("还没登录？先去登录")
                    }
                }
            }

            item {
                TechFlowFooter()
            }
        }
    }

    if (showHelpDialog) {
        AskHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun EditorBlock(
    title: String,
    body: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        body()
    }
}

@Composable
private fun MarkdownToolbar(
    onInsert: ((TextFieldValue) -> TextFieldValue) -> Unit,
    onOpenHelp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column {
            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolbarButton("Aa") { onInsert { insertAround(it, "", "", "正文") } }
                ToolbarButton("B") { onInsert { insertAround(it, "**", "**", "加粗文本") } }
                ToolbarButton("I") { onInsert { insertAround(it, "_", "_", "斜体文本") } }
                ToolbarButton("</>") { onInsert { insertAround(it, "```\n", "\n```", "# include") } }
                ToolbarButton("链接") { onInsert { insertAround(it, "[", "](https://url.com)", "标题") } }
                ToolbarButton("引用") { onInsert { insertPrefix(it, "> ") } }
                ToolbarButton("图片") { onInsert { insertAround(it, "![image.png](", ")", "https://image.url") } }
                ToolbarButton("列表") { onInsert { insertPrefix(it, "- ") } }
                ToolbarButton("?") { onOpenHelp() }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun AskHelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        },
        title = {
            Text("如何排版", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("• 引用问题或答案：`#10010000000000001`")
                Text("• 添加链接：`<https://url.com>` 或 `[标题](https://url.com)`")
                Text("• 段落之间使用空行分隔")
                Text("• `_斜体_` 或者 `**粗体**`")
                Text("• 在行首添加 `> ` 表示引用")
                Text("• 使用三引号创建代码块：\n```\n这是代码块\n```")
            }
        },
    )
}

private fun insertAround(
    current: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val range = current.selection
    val before = current.text.substring(0, range.start)
    val selected = current.text.substring(range.start, range.end).ifBlank { placeholder }
    val after = current.text.substring(range.end)
    val inserted = prefix + selected + suffix
    val newText = before + inserted + after
    val cursor = before.length + inserted.length
    return TextFieldValue(newText, selection = TextRange(cursor))
}

private fun insertPrefix(
    current: TextFieldValue,
    prefix: String,
): TextFieldValue {
    val range = current.selection
    val before = current.text.substring(0, range.start)
    val selected = current.text.substring(range.start, range.end).ifBlank { "内容" }
    val after = current.text.substring(range.end)
    val inserted = prefix + selected
    val newText = before + inserted + after
    val cursor = before.length + inserted.length
    return TextFieldValue(newText, selection = TextRange(cursor))
}
