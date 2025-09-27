package com.example.localfirstassistant.ui.ocr

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.localfirstassistant.data.db.AppDatabase
import com.example.localfirstassistant.data.db.entities.MessageEntity
import com.example.localfirstassistant.data.db.entities.TaskSource
import com.example.localfirstassistant.data.db.entities.TaskMessageCrossRef
import com.example.localfirstassistant.data.parse.OcrParser
import com.example.localfirstassistant.data.parse.TaskDraft
import com.example.localfirstassistant.data.parse.TaskNormalizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Handles ML Kit OCR flow for gallery images and persists user-confirmed tasks.
 */
class OcrActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "localfirst.db").build()
    }

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var pendingDraftConsumer: ((TaskDraft?) -> Unit)? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) {
            pendingDraftConsumer?.invoke(null)
            return@registerForActivityResult
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val image = InputImage.fromInputStream(stream, 0)
                    val result = recognizer.process(image).await()
                    val draft = OcrParser.parse(result.text)
                    launch(Dispatchers.Main) {
                        pendingDraftConsumer?.invoke(draft)
                    }
                } ?: launch(Dispatchers.Main) {
                    Toast.makeText(this@OcrActivity, "이미지를 불러오지 못했습니다", Toast.LENGTH_LONG).show()
                    pendingDraftConsumer?.invoke(null)
                }
            } catch (t: Throwable) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@OcrActivity, "OCR 실패: ${t.message}", Toast.LENGTH_LONG).show()
                    pendingDraftConsumer?.invoke(null)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OcrScreen(onPickImage = { consumer -> startOcrFlow(consumer) }, onSave = ::persistDraft)
                }
            }
        }
    }

    private fun startOcrFlow(consumer: (TaskDraft?) -> Unit) {
        pendingDraftConsumer = consumer
        galleryLauncher.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)
    }

    private fun persistDraft(draft: TaskDraft) {
        lifecycleScope.launch(Dispatchers.IO) {
            val messageId = database.messageDao().insert(
                MessageEntity(
                    accountType = TaskSource.OCR.name,
                    messageId = "OCR-${System.currentTimeMillis()}",
                    subject = draft.title,
                    sender = null,
                    receivedAt = Instant.now(),
                    body = draft.description
                )
            )
            val normalized = TaskNormalizer.normalize(
                title = draft.title,
                body = draft.description,
                source = TaskSource.OCR,
                reference = Instant.now(),
                resolvedInstant = draft.dueAt?.toInstant(),
                scoreOverride = draft.score,
                bucketOverride = draft.bucket
            )
            val taskId = database.taskDao().upsert(normalized)
            database.taskMessageDao().insert(
                TaskMessageCrossRef(
                    task_id = taskId,
                    message_id = messageId
                )
            )
            launch(Dispatchers.Main) {
                Toast.makeText(this@OcrActivity, "작업이 저장되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

}

@Composable
private fun OcrScreen(onPickImage: ((TaskDraft?) -> Unit) -> Unit, onSave: (TaskDraft) -> Unit) {
    val draftState = remember { mutableStateOf<TaskDraft?>(null) }
    val titleState = remember { mutableStateOf(TextFieldValue("")) }
    val descState = remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "이미지에서 작업 추출", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = {
            onPickImage { draft ->
                draftState.value = draft
                titleState.value = TextFieldValue(draft?.title.orEmpty())
                descState.value = TextFieldValue(draft?.description.orEmpty())
            }
        }) {
            Text("갤러리에서 선택")
        }
        draftState.value?.let { draft ->
            OutlinedTextField(
                value = titleState.value,
                onValueChange = { titleState.value = it },
                label = { Text("제목") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descState.value,
                onValueChange = { descState.value = it },
                label = { Text("내용") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
            Text(text = "예측 점수: %.2f".format(draft.score))
            draft.dueAt?.let {
                Text(text = "예상 기한: ${it.toLocalDate()} ${it.toLocalTime()} (KST)")
            }
            Button(
                onClick = {
                    val confirmed = draft.copy(
                        title = titleState.value.text,
                        description = descState.value.text
                    )
                    onSave(confirmed)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장")
            }
        } ?: run {
            Text(text = "이미지를 선택하면 작업 후보가 표시됩니다.")
        }
    }
}
