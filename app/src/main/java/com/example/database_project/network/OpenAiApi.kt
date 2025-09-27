package com.example.database_project.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun getCompletion(@Body request: OpenAiRequest): OpenAiResponse
}
