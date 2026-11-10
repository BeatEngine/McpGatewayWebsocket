package org.beatengine.mcpgateway.aiintegration;

import io.micrometer.observation.ObservationRegistry;
import org.beatengine.mcpgateway.McpGatewayApplication;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Properties;

/**
 * AI-LLM Configurations
 * OLLAMA: <a href="https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html">docs.spring.io ollama-chat</a>
 */
@Configuration
public class ChatClientConfigurations {

    /**
     * @return The Spring properties loaded at start of the application. <p></p>
     *  spring.ai.ollama.base-url, spring.ai.ollama.chat.model
     */
    public static Properties getAiConfiguration()
    {
        final Properties properties = new Properties();
        //McpGatewayApplication.loadRootProjectEnvironmentVariables();

        final String burl = System.getProperty("OLLAMA_URL");
        properties.put("spring.ai.ollama.base-url", burl);
        System.getLogger("Configurations").log(System.Logger.Level.ALL, "Set spring.ai.ollama.base-url = " + burl);

        final String model = System.getProperty("OLLAMA_MODEL");
        properties.put("spring.ai.ollama.chat.model", model);
        System.getLogger("Configurations").log(System.Logger.Level.INFO, "Set spring.ai.ollama.chat.model = " + model);

        return properties;
    }

    @Bean
    public OllamaApi ollamaApi() {
        final String burl = System.getProperty("OLLAMA_URL");
        System.getLogger("Configurations").log(System.Logger.Level.ALL, "OllamaApi baseUrl = " + burl);

        // Synchronous HTTP client set http timeout to 5 minutes
        final SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofMinutes(5));

        final RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

        // Reactive HTTP client set http timeout to 5 minutes
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));

        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                );

        return OllamaApi.builder()
                .baseUrl(burl)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }



    /* Further options
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel chatModel, ChatClientBuilderConfigurer configurer,
                                       ObjectProvider<ObservationRegistry> observationRegistry,
                                       ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
                                       ObjectProvider<AdvisorObservationConvention> advisorObservationConvention,
                                       ObjectProvider<ToolCallingAdvisor.Builder<?>> toolCallingAdvisorBuilder) {
        return buildChatClient(chatModel, configurer, observationRegistry,
                chatClientObservationConvention, advisorObservationConvention, toolCallingAdvisorBuilder);
    }

    private ChatClient buildChatClient(ChatModel chatModel, ChatClientBuilderConfigurer configurer,
                                       ObjectProvider<ObservationRegistry> observationRegistry,
                                       ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
                                       ObjectProvider<AdvisorObservationConvention> advisorObservationConvention,
                                       ObjectProvider<ToolCallingAdvisor.Builder<?>> toolCallingAdvisorBuilder) {
        ChatClient.Builder builder = ChatClient.builder(chatModel,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                chatClientObservationConvention.getIfUnique(),
                advisorObservationConvention.getIfUnique(),
                toolCallingAdvisorBuilder.getIfAvailable());
        return configurer.configure(builder).build();
    }*/

}
