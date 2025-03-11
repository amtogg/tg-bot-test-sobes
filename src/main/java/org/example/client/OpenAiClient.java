package org.example.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.GptRequest;
import org.example.dto.GptResponse;
import org.example.dto.Transcription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.chat.url}")
    private String chatApiUrl;

    @Value("${openai.api.chat.model}")
    private String chatModel;

    @Value("${openai.api.chat.system_role}")
    private String systemRole;

    @Value("${openai.api.transcription.url}")
    private String transcriptionApiUrl;

    @Value("${openai.api.transcription.model}")
    private String voiceModel;

    @Value("${openai.api.transcription.language}")
    private String language;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String promptModel(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        GptRequest body = GptRequest.builder()
                .model(chatModel)
                .messages(List.of(
                        GptRequest.Message.builder()
                                .role("system")
                                .content(systemRole)
                                .build(),
                        GptRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();

        HttpEntity<GptRequest> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(chatApiUrl, request, String.class);
        GptResponse responseBody;
        try {
            responseBody = objectMapper.readValue(response.getBody(), GptResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("There's an error when parsing JSON response from GPT", e);
        }
        return responseBody.getChoices().get(0).getMessage().getContent();
    }

    public String transcribe(File audio) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        FileSystemResource fileResource = new FileSystemResource(audio);
        body.add("file", fileResource);
        body.add("model", voiceModel);
        body.add("language", language);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(transcriptionApiUrl, requestEntity, String.class);
        Transcription transcription;
        try {
            transcription = objectMapper.readValue(response.getBody(), Transcription.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("There was an error when converting JSON response to DTO", e);
        }
        return transcription.text();
    }
}