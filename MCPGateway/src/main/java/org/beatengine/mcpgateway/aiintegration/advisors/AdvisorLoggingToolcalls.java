package org.beatengine.mcpgateway.aiintegration.advisors;


import org.beatengine.mcpgateway.McpGatewayApplication;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AdvisorLoggingToolcalls implements BaseAdvisor {

    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private static final int DEFAULT_ORDER = 1000;
    private final int order;

    public AdvisorLoggingToolcalls() {
        this(DEFAULT_ORDER);
    }

    public AdvisorLoggingToolcalls(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        List<String> toolNames = new ArrayList<>();

        if (chatClientRequest.prompt().getOptions() instanceof ToolCallingChatOptions options
                && options.getToolCallbacks() != null) {
            toolNames = options.getToolCallbacks().stream()
                    .map(tc -> tc.getToolDefinition().name())
                    .sorted()
                    .toList();
        }

        Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINE,CYAN+"LLM call: "
                +toolNames.size()+" tool(s) visible to the model: "+ toolNames+RESET);

        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        var chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            return chatClientResponse;
        }

        chatResponse.getResults().stream()
                .map(Generation::getOutput)
                .filter(message -> !message.getToolCalls().isEmpty())
                .flatMap(message -> message.getToolCalls().stream())
                .forEach(toolCall ->
                    Logger.getLogger(this.getClass().getSimpleName()).log(McpGatewayApplication.FINER,GREEN+
                            "Model called: "+toolCall.name()+" (args: "+truncate(toolCall.arguments(), 160)+")"+RESET)
                );

        return chatClientResponse;
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}