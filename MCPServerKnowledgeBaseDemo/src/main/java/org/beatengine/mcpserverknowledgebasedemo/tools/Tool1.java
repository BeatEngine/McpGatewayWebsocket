package org.beatengine.mcpserverknowledgebasedemo.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;


public class Tool1 {

    @Tool(description = "Search in knowledge base for a keyword and get a description of that!")
    public String searchInKnowledgeBase(@ToolParam(description = "Search for the keyword") final String keyword) {
        return "This is a demo mcp server. " +
               "You can implement it to look for the keyword '" + keyword + "' in real documents!";
    }

}
