@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.example.database_project

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.example.database_project.entity.*
import com.example.database_project.network.Message
import com.example.database_project.network.OpenAiApi
import com.example.database_project.network.OpenAiRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.format.DateUtils

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd(E) HH:mm", Locale.KOREA)

/** Long( epoch millis ) → “yyyy-MM-dd(요일) HH:mm” */
fun Long.formatAsLocal(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)

/** Long → “n분 전 / n시간 전 …” 같은 상대 시간 */
fun Long.formatAsRelative(): CharSequence =
    DateUtils.getRelativeTimeSpanString(
        this, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
    )
// OpenAI 응답 파싱용 임시 DTO
@Serializable
data class AiEmailParsed(
    val title: String,
    val sender: String,
    val summary: String,
    val category: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrofit: OpenAI
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
            )
            .build()
        val openAiApi = retrofit.create(OpenAiApi::class.java)

        setContent {
            Column(Modifier.padding(16.dp)) {

                Button(onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // Gmail에서 온 것처럼 가정한 샘플 데이터
                            val fromName = "이대리"
                            val fromEmail = "edaeri@example.com"
                            val subject = "AI 프로젝트 회의 일정"
                            val encodedBody = "7JWI64WV7ZWY7IS47JqUIO2UhOuhnOygne2KuCDtmozsnZjrpbwg7KeE7ZaJ7ZWY6rOgIOyLtuyKteuLiOuLpCDsnbTrjIDrpqwg65Oc66a8Lg==\n"
                            val body = String(
                                Base64.decode(encodedBody, Base64.URL_SAFE or Base64.NO_WRAP),
                                Charsets.UTF_8
                            )

                            val prompt = """
                                아래 이메일(제목/본문)을 JSON으로 정리해줘.
                                JSON 키는 반드시 title, sender, summary, category 이어야 해.
                                category는 '업무', '광고', '개인' 중 하나로만 분류해.
                                ---
                                제목: $subject
                                본문:
                                $body
                                ---
                                보낸 사람 이름은 가능하면 '$fromName'로 두어라.
                            """.trimIndent()

                            val request = OpenAiRequest(
                                model = "gpt-3.5-turbo",
                                messages = listOf(Message("user", prompt))
                            )
                            val response = openAiApi.getCompletion(
                                "Bearer ${BuildConfig.OPENAI_API_KEY}", request
                            )
                            val raw = response.choices.first().message.content
                            val s = raw.indexOf('{'); val e = raw.lastIndexOf('}')
                            require(s != -1 && e != -1 && e > s) { "JSON 추출 실패: $raw" }
                            val jsonText = raw.substring(s, e + 1)

                            val parsed = Json.decodeFromString(AiEmailParsed.serializer(), jsonText)

                            // 정규화 저장
                            App.db.withTransaction {
                                val userId = App.db.userDao().getUserByName("내 계정")?.id
                                    ?: App.db.userDao().insert(User(name = "내 계정", address = ""))

                                val typeId = App.db.eventTypeDao().getByName(parsed.category)?.id
                                    ?: App.db.eventTypeDao().insert(EventType(typeName = parsed.category, subdivision = null))

                                val contactId = App.db.contactDao().getByName(parsed.sender)?.id
                                    ?: App.db.contactDao().insert(Contact(name = parsed.sender, email = fromEmail, phoneNumber = null))

                                App.db.eventDao().insert(
                                    Event(
                                        userId = userId,
                                        eventTypeId = typeId,
                                        contactId = contactId,
                                        timestamp = System.currentTimeMillis(),
                                        originalContent = "제목: ${parsed.title}\n\n본문:\n$body",
                                        summary = parsed.summary
                                    )
                                )
                            }

                        } catch (t: Throwable) {
                            Log.e("Main", "실패", t)
                        }
                    }
                }) { Text("샘플 Gmail → OpenAI → 정규화 DB 저장") }

                // 리스트 표시 (Flow → State)
                val events by App.db.eventDao().getAll().collectAsState(initial = emptyList())

                LazyColumn(Modifier.padding(top = 16.dp)) {
                    items(events) { ev: com.example.database_project.entity.Event ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("시간: ${ev.timestamp.formatAsLocal()}  (${ev.timestamp.formatAsRelative()})")
                            Text("요약", fontWeight = FontWeight.Bold)
                            Text(ev.summary)
                            Text("")
                            Text("원문", fontWeight = FontWeight.Bold)
                            Text(ev.originalContent)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
