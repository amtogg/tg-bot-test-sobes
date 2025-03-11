package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.client.OpenAiClient;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;


@RequiredArgsConstructor
public abstract class Command {

    protected final TopicRepository topicRepository;
    protected final OpenAiClient openAiClient;
    protected final InterviewRepository interviewRepository;

    public abstract boolean isApplicable(Update update);

    public abstract String process(Update update, Bot bot);
}
