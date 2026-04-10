package com.example.workshop6.data.api;

import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.example.workshop6.data.api.dto.SendMessageRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface ChatApiService {

    @GET("api/v1/chat/threads/me/open")
    Call<ChatThreadDto> getMyThread();

    @GET("api/v1/chat/threads/{threadId}/messages")
    Call<List<ChatMessageDto>> getMessages(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/messages")
    Call<ChatMessageDto> sendMessage(
            @Path("threadId") int threadId,
            @Body SendMessageRequest body
    );
}