package org.beatengine.mcpgateway.aiintegration.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is for injecting a context (parameters) for other advisors.
 * e.g. FIX for the MessageChatMemoryAdvisor context has not found ChatMemory.CONVERSATION_ID this had to be passed as a
 * context option because of a design problem in the DefaultClientImplementation of MessageChatMemoryAdvisor.
 * It's missing that advisors can pass an own context etc.
 *
 */
public class AdvisorContextInjectionAdvisor  implements CallAdvisor {

    private final Map<String, Object> contextParams = new HashMap<>();

    // Provide a method to inject runtime parameters dynamically
    public void setInjectAdvisorParams(Map<String, Object> advisorParams) {
        if (advisorParams != null) {
            this.contextParams.putAll(advisorParams);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        chatClientRequest.context().putAll(contextParams);
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
