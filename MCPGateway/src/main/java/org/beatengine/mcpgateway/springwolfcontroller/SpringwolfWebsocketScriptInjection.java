package org.beatengine.mcpgateway.springwolfcontroller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class SpringwolfWebsocketScriptInjection implements WebMvcConfigurer {

    private final String javascriptContent;

    public SpringwolfWebsocketScriptInjection(ResourceLoader resourceLoader) {
        String content;
        try {
            Resource resource = resourceLoader.getResource("classpath:static/js/springwolf-websocket.js");
            content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO, "Resource springwolf-websocket.js loaded!");
        } catch (IOException e) {
            content = "document.alert('Springwolf JS-Injection ws publisher failed!');";
        }
        this.javascriptContent = "<script>" + content + "</script>";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // INJECTION OF SCRIPT INTO /springwolf/asyncapi-ui.html
        registry.addResourceHandler("/springwolf/**")
                .addResourceLocations("classpath:/META-INF/resources/springwolf/")
                .resourceChain(true)
                .addTransformer(new ResourceTransformer() {
                    @Override
                    public Resource transform(HttpServletRequest request, Resource resource,
                                              ResourceTransformerChain transformerChain) throws IOException {

                        // INJECTION OF SCRIPT INTO /springwolf/asyncapi-ui.html
                        if (resource.getFilename() != null && resource.getFilename().equals("asyncapi-ui.html")) {
                            String originalHtml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                            String modifiedHtml = originalHtml.replace("</body>", javascriptContent + "</body>");
                            System.getLogger(this.getClass().getSimpleName()).log(System.Logger.Level.INFO,"Injected springwolf-websocket.js into " + resource.getFilename());
                            return new org.springframework.web.servlet.resource.TransformedResource(resource, modifiedHtml.getBytes(StandardCharsets.UTF_8));
                        }

                        return transformerChain.transform(request, resource);
                    }
                });
    }

}

