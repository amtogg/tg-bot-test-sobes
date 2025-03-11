package org.example.service;


import org.example.client.OpenAiClient;
import org.example.dto.Question;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;

@Component
public class VoiceCommand extends Command {

    private static final String QUESTION_PROMPT = """
                Вот исходный вопрос для собеседования на Java Junior в Java: %s
                Задай его, придумав интересную ситуацию c каким-нибудь реальным очень известным
                приложением, в контексте которого и будет задаваться вопрос. Чтобы это не выглядело сухо, как
                просто вопрос по Java. Подумай, как можно сделать его более интересным и понятным для кандидата
                с помощью дополнительного контекста реального приложения.
                
                Учитывай, что кандидат пока может не знать
                про веб-приложения и сложную разработку. Но он знает про популярные приложения, которые есть
                в мире, такие как YouTube, Tinder, Twitter или Telegram. Но не используй Meta, Facebook или
                Instagram в качестве своих примеров. Прочие же приложения могут участвовать в формировании
                контекста для твоих вопросов, чтобы сделать собеседование более интересным для кандидата.
        
                Пример: Представим, что мы работаем в Google над приложением YouTube. И нам поручают задачу:
                создать класс видео, в котором будет 3 поля: название, количество лайков и количество просмотров.
                Добавляя эти поля, нам важно соблюсти принцип инкапсуляции в ООП. Что это такое, и как он
                реализуется в Java?
                
                Используй и другие примеры известных приложений, не только YouTube. Но иногда можно и YouTube.
                Кроме того, пример выше - про инкапсуляцию. Но ты задавай вопрос конкретно на ту тему, что
                указана в самом начале промпта. Пример используй только для понимания, как мог бы выглядеть
                вопрос по стилю и контексту, но его смысловое наполнение должно относиться именно к выбранной
                выше теме.
                
                Стиль общения:
                Общайся с кандидатом на "ты". Это должна быть беседа двух хороших друзей, тепло и непринужденно,
                без лишних формальностей.
                Избегай слишком многословных формулировок. Старайся формулировать предложения четко и по делу,
                но в то же время сохраняй ламповость и живость беседы. Нужно найти баланс. Тем не менее,
                человек не должен получить от тебя огромный текст, который ему будет лень читать. Разговор
                должен быть интересным, но лаконичным, чтобы человек не потерял желание его продолжать из-за
                огромного количества текста на экране.
                """;
    private static final String FEEDBACK_PROMPT = """
                Проанализируй вопросы на собеседовании для позиции Junior Java Developer и те ответы, которые на каждый
                из них дал кандидат и предоставь обратную связь для кандидата по тому, насколько хорошо ему удалось ответить
                на эти вопросы именно для этого уровня собеседования.
                Каждому вопросу соответствует ответ, который следует сразу за ним. Вопросы могут
                быть не связаны друг с другом, поэтому делай оценку относительно ответов, данных на конкретные вопросы,
                а не всех вместе.
                
                Давая обратную связь, сначала расскажи о том, что у кандидата получилось хорошо, какие темы он знает
                действительно глубоко и где конкретно ему удалось показать очень хороший уровень.
                                
                После обрати внимание на ответы, которые были неверны или верны лишь наполовину. Конкретно укажи, где
                кандидат ошибся в своих утверждениях на тот или иной вопрос и опиши, как он мог бы улучшить свой ответ.
                Давай достаточно подробную обратную связь в этом месте, чтобы кандидат четко понимал свои ошибки и мог
                сделать из них выводы и чему-то научиться буквально из этой обратной связи.
                                
                Затем подскажи кандидату, на какие темы он мог бы обратить больше внимания при дальнейшей подготовке,
                чтобы заполнить те пробелы, которые были обнаружены у него в процессе того собеседования, что ты
                анализируешь.
                                
                Стиль общения:
                Общайся с кандидатом на "ты". Это должна быть беседа двух хороших друзей, тепло и непринужденно,
                без лишних формальностей.
                Избегай слишком многословных формулировок. Старайся формулировать предложения четко и по делу,
                но в то же время сохраняй ламповость и живость беседы. Нужно найти баланс. Тем не менее,
                человек не должен получить от тебя огромный текст, который ему будет лень читать. Разговор
                должен быть интересным, но лаконичным, чтобы человек не потерял желание его продолжать из-за
                огромного количества текста на экране.
                                
                Форматирование:
                Разделяй свою обратную связь на абзацы после каждых 4-5 предложений. Отдели блоки "Что получилось хорошо",
                "Что можно улучшить" и "На что обратить внимание" соответствующими заголовками отдельными от основного
                содержимого этих блоков.
                                
                Результаты собеседования:
                                
            """;

    @Value("${interview.max-questions}")
    private int maxQuestions;

    public VoiceCommand(OpenAiClient openAiClient,
                        InterviewRepository interviewRepository,
                        TopicRepository topicRepository) {
        super(topicRepository, openAiClient, interviewRepository);
    }

    @Override
    public boolean isApplicable(Update update) {
        return update.getMessage().hasVoice();
    }

    @Override
    public String process(Update update, Bot bot) {
        String answer = transcribeVoiceAnswer(update, bot);
        String userName = update.getMessage().getFrom().getUserName();
        interviewRepository.addAnswer(userName, answer);
        if (interviewRepository.getUserQuestions(userName) == maxQuestions) {
            return provideFeedback(userName);
        } else {
            return askNextQuestion(userName);
        }
    }

    private String transcribeVoiceAnswer(Update update, Bot bot) {
        Voice voice = update.getMessage().getVoice();
        String fileId = voice.getFileId();
        java.io.File audio;
        try {
            GetFile getFileRequest = new GetFile();
            getFileRequest.setFileId(fileId);
            File file = bot.execute(getFileRequest);

            audio = bot.downloadFile(file.getFilePath());
        } catch (TelegramApiException e) {
            throw new IllegalStateException("There's an error when processing Telegram audio", e);
        }
        return openAiClient.transcribe(renameToOgg(audio));
    }

    private java.io.File renameToOgg(java.io.File tmpFile) {
        String fileName = tmpFile.getName();
        String newFileName = fileName.substring(0, fileName.length() - 4) + ".ogg";
        Path sourcePath = tmpFile.toPath();
        Path targetPath = sourcePath.resolveSibling(newFileName);
        try {
            Files.move(sourcePath, targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("There was an error when renaming .tmp audio file to .ogg", e);
        }
        return targetPath.toFile();
    }

    private String askNextQuestion(String userName) {
        String prompt = String.format(QUESTION_PROMPT, topicRepository.getRandomTopic());
        String question = openAiClient.promptModel(prompt);
        interviewRepository.addQuestion(userName, question);
        return question;
    }

    private String provideFeedback(String userName) {
        StringBuilder feedbackPrompt = new StringBuilder();
        feedbackPrompt.append(FEEDBACK_PROMPT);
        Deque<Question> questions = interviewRepository.finishInterview(userName);
        questions.forEach(question -> feedbackPrompt.append("Исходный вопрос: ")
                .append(question.getQuestion()).append("\n")
                .append("Ответ кандидата: ")
                .append(question.getAnswer()).append("\n"));
        return openAiClient.promptModel(feedbackPrompt.toString());
    }
}
