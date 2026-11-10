package org.beatengine.mcpgateway;

import io.github.springwolf.asyncapi.v3.bindings.ServerBinding;
import io.github.springwolf.asyncapi.v3.model.ExtendableObject;
import io.github.springwolf.asyncapi.v3.model.ExternalDocumentation;
import io.github.springwolf.asyncapi.v3.model.Tag;
import io.github.springwolf.asyncapi.v3.model.info.Contact;
import io.github.springwolf.asyncapi.v3.model.info.Info;
import io.github.springwolf.asyncapi.v3.model.info.License;
import io.github.springwolf.asyncapi.v3.model.server.Server;
import io.github.springwolf.core.configuration.docket.AsyncApiDocket;
import io.github.springwolf.core.configuration.docket.AsyncApiDocketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class SpringWolfConfig {

    @Value("${server.port}")
    private String serverPort;

    @Value("${springwolf.docket.base-package}")
    private String basePacket;

    @Value("${springwolf.docket.info.title}")
    private String title;

    @Value("${springwolf.docket.info.version}")
    private String version;

    @Value("${springwolf.docket.info.description}")
    private String description;

    @Bean
    public AsyncApiDocketService customAsyncApiDocketService() {



        Server wsServer = Server.builder()
                .host("localhost:"+serverPort)
                .protocol("ws")
                .description("Central websocket entry-point.")
                .pathname("ws")
                .build();

        return new AsyncApiDocketService() {
            @Override
                public AsyncApiDocket getAsyncApiDocket() {
                final AsyncApiDocket asyncApiDocket = AsyncApiDocket.builder()
                        .basePackage(basePacket)
                        .info(Info.builder().title(title).version(version).description(description).build())
                        .servers(Map.of("ws", wsServer))
                        .build();

                return asyncApiDocket;
            }
        };
    }



}
