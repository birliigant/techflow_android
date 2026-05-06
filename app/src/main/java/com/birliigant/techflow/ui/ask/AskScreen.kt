@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.birliigant.techflow.ui.ask

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    AskPartition("考研保研", "graduate"),
    AskPartition("实习就业", "internship"),
    AskPartition("校内作业", "homework"),
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
    val isUploadingImage: Boolean = false,
    val message: String? = null,
)

class AskViewModel(
    private val questionRepository: QuestionRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val editorState = MutableStateFlow(AskUiState())
    private val submitEvents = MutableSharedFlow<Unit>()
    private val editorInsertEvents = MutableSharedFlow<String>()

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
    val editorInsertions = editorInsertEvents.asSharedFlow()

    fun updateTitle(value: String) = editorState.update { it.copy(title = value) }

    fun updateContent(value: String) = editorState.update { it.copy(content = value) }

    fun updateTags(value: String) = editorState.update { it.copy(tagsInput = value) }

    fun updatePartition(value: String) = editorState.update { it.copy(partition = value) }

    fun resetDraft() {
        editorState.value = AskUiState(isLoggedIn = uiState.value.isLoggedIn)
    }

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun uploadImage(
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
    ) {
        viewModelScope.launch {
            editorState.update { it.copy(isUploadingImage = true, message = null) }
            val result = questionRepository.uploadPostImage(
                fileName = fileName,
                bytes = bytes,
                mimeType = mimeType,
            )
            if (result.isSuccess) {
                val fileLabel = fileName.substringBeforeLast('.').ifBlank { "image" }
                editorInsertEvents.emit("![$fileLabel](${result.getOrThrow()})")
                editorState.update {
                    it.copy(
                        isUploadingImage = false,
                        message = "图片已上传并插入正文。",
                    )
                }
            } else {
                editorState.update {
                    it.copy(
                        isUploadingImage = false,
                        message = result.exceptionOrNull()?.message ?: "图片上传失败",
                    )
                }
            }
        }
    }

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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var contentValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.content))
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                snackbarHostState.showSnackbar("读取图片失败，请重试。")
                return@launch
            }
            viewModel.uploadImage(
                fileName = context.resolveDisplayName(uri),
                bytes = bytes,
                mimeType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/*" },
            )
        }
    }

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

    LaunchedEffect(viewModel) {
        viewModel.editorInsertions.collect { snippet ->
            contentValue = insertRawSnippet(contentValue, snippet)
            viewModel.updateContent(contentValue.text)
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1F242B),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                MarkdownToolbar(
                                    isUploadingImage = uiState.isUploadingImage,
                                    onInsert = { operation ->
                                        contentValue = operation(contentValue)
                                        viewModel.updateContent(contentValue.text)
                                    },
                                    onPickImage = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onOpenHelp = {
                                        uriHandler.openUri("https://commonmark.org/help/")
                                    },
                                )
                                OutlinedTextField(
                                    value = contentValue,
                                    onValueChange = {
                                        contentValue = it
                                        viewModel.updateContent(it.text)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 12,
                                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF3F4F6),
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1F242B),
                                        unfocusedContainerColor = Color(0xFF1F242B),
                                        focusedBorderColor = Color(0x00000000),
                                        unfocusedBorderColor = Color(0x00000000),
                                        cursorColor = Color(0xFFF3F4F6),
                                    ),
                                    placeholder = {
                                        Text(
                                            text = "使用 Markdown 输入内容，下面会实时预览。",
                                            color = Color(0xFF97A0AF),
                                        )
                                    },
                                )
                            }
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
                                placeholder = { Text("可选，多个标签用逗号分隔；不填时自动使用“其他”") },
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
                            color = Color(0xFF151A20),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            if (uiState.content.isBlank()) {
                                Text(
                                    text = "开始输入正文后，这里会实时展示 Markdown 预览。",
                                    modifier = Modifier.padding(18.dp),
                                    color = Color(0xFF97A0AF),
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    MarkdownText(
                                        content = uiState.content,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color(0xFFF3F4F6),
                                        ),
                                    )
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
                        enabled = !uiState.isSubmitting && !uiState.isUploadingImage,
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
    isUploadingImage: Boolean,
    onInsert: ((TextFieldValue) -> TextFieldValue) -> Unit,
    onPickImage: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF242A31),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ToolbarButton("Aa", Color(0xFFF3F4F6)) { onInsert { insertLinePrefix(it, "# ", "标题") } }
                ToolbarButton("B", Color(0xFFF3F4F6)) { onInsert { wrapSelection(it, "**", "**", "粗体文本") } }
                ToolbarButton("I", Color(0xFFF3F4F6)) { onInsert { wrapSelection(it, "_", "_", "斜体文本") } }
                ToolbarSeparator()
                ToolbarButton("</>", Color(0xFFF3F4F6)) { onInsert { insertCodeBlock(it) } }
                ToolbarButton("链", Color(0xFFF3F4F6)) { onInsert { insertLink(it) } }
                ToolbarButton("引", Color(0xFFF3F4F6)) { onInsert { insertLinePrefix(it, "> ", "引用内容") } }
                ToolbarButton(if (isUploadingImage) "上传中" else "图", if (isUploadingImage) Color(0xFF97A0AF) else Color(0xFFF3F4F6), enabled = !isUploadingImage, onClick = onPickImage)
                ToolbarButton("表", Color(0xFFF3F4F6)) { onInsert { insertTable(it) } }
                ToolbarSeparator()
                ToolbarButton("1.", Color(0xFFF3F4F6)) { onInsert { insertOrderedList(it) } }
                ToolbarButton("•", Color(0xFFF3F4F6)) { onInsert { insertUnorderedList(it) } }
                ToolbarButton(">>", Color(0xFFF3F4F6)) { onInsert { indentSelection(it) } }
                ToolbarButton("<<", Color(0xFFF3F4F6)) { onInsert { outdentSelection(it) } }
                ToolbarButton("—", Color(0xFFF3F4F6)) { onInsert { insertHorizontalRule(it) } }
                ToolbarSeparator()
                ToolbarButton("?", Color(0xFFF3F4F6), onClick = onOpenHelp)
            }
            HorizontalDivider(color = Color(0xFF3A414B))
        }
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun ToolbarSeparator(
) {
    Text(
        text = "|",
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
        color = Color(0xFF5B6572),
        style = MaterialTheme.typography.titleMedium,
    )
}

private fun wrapSelection(
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

private fun insertRawSnippet(
    current: TextFieldValue,
    snippet: String,
): TextFieldValue {
    val range = current.selection
    val before = current.text.substring(0, range.start)
    val after = current.text.substring(range.end)
    val newText = before + snippet + after
    val cursor = before.length + snippet.length
    return TextFieldValue(newText, selection = TextRange(cursor))
}

private fun insertLinePrefix(
    current: TextFieldValue,
    prefix: String,
    placeholder: String,
): TextFieldValue {
    return transformSelectedLines(current) { lines ->
        lines.map { line ->
            prefix + line.ifBlank { placeholder }
        }
    }
}

private fun insertOrderedList(current: TextFieldValue): TextFieldValue {
    return transformSelectedLines(current) { lines ->
        lines.mapIndexed { index, line ->
            "${index + 1}. ${line.ifBlank { "列表项 ${index + 1}" }}"
        }
    }
}

private fun insertUnorderedList(current: TextFieldValue): TextFieldValue {
    return transformSelectedLines(current) { lines ->
        lines.mapIndexed { index, line ->
            "- ${line.ifBlank { "列表项 ${index + 1}" }}"
        }
    }
}

private fun indentSelection(current: TextFieldValue): TextFieldValue {
    return transformSelectedLines(current) { lines ->
        lines.map { "    $it" }
    }
}

private fun outdentSelection(current: TextFieldValue): TextFieldValue {
    return transformSelectedLines(current) { lines ->
        lines.map { line ->
            when {
                line.startsWith("    ") -> line.removePrefix("    ")
                line.startsWith("\t") -> line.removePrefix("\t")
                else -> line
            }
        }
    }
}

private fun insertCodeBlock(current: TextFieldValue): TextFieldValue {
    return wrapSelection(current, "```\n", "\n```", "这是代码块")
}

private fun insertLink(current: TextFieldValue): TextFieldValue {
    return wrapSelection(current, "[", "](https://url.com)", "链接标题")
}

private fun insertTable(current: TextFieldValue): TextFieldValue {
    val table = """
| 列 1 | 列 2 |
| --- | --- |
| 内容 1 | 内容 2 |
""".trimIndent()
    return insertRawSnippet(current, table)
}

private fun insertHorizontalRule(current: TextFieldValue): TextFieldValue {
    val insertion = if (current.text.isBlank() || current.text.endsWith("\n")) {
        "---\n"
    } else {
        "\n---\n"
    }
    return insertRawSnippet(current, insertion)
}

private fun transformSelectedLines(
    current: TextFieldValue,
    transform: (List<String>) -> List<String>,
): TextFieldValue {
    val text = current.text
    val range = current.selection
    val lineStart = text.lastIndexOf('\n', startIndex = (range.start - 1).coerceAtLeast(0)).let {
        if (it == -1 || range.start == 0) 0 else it + 1
    }
    val lineEnd = text.indexOf('\n', startIndex = range.end).let {
        if (it == -1) text.length else it
    }
    val selectedBlock = text.substring(lineStart, lineEnd)
    val lines = selectedBlock.split('\n')
    val transformed = transform(lines).joinToString("\n")
    val newText = text.substring(0, lineStart) + transformed + text.substring(lineEnd)
    return TextFieldValue(newText, selection = TextRange(lineStart + transformed.length))
}

private fun Context.resolveDisplayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex).orEmpty().ifBlank { "image_${System.currentTimeMillis()}.png" }
        }
    }
    return "image_${System.currentTimeMillis()}.png"
}
