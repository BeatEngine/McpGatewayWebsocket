package org.beatengine.mcpgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sound.sampled.LineEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.logging.*;

@SpringBootApplication
public class McpGatewayApplication {

    public static String ROOT_ENVIRONMENT_PATH = new File(Paths.get("").toAbsolutePath().toFile(), "local.env").getAbsolutePath();

    // Workaround set logging by environment 'MCP_GATEWAY_LOGGING'
    public static Level FINE = Level.FINE;
    public static Level FINER = Level.FINER;
    public static Level INFO = Level.INFO;

    public static void main(String[] args) {
        // Transfer the environment into the properties or load testing .env
        McpGatewayApplication.loadRootProjectEnvironmentVariables();

        initLogging();

        SpringApplication.run(McpGatewayApplication.class, args);
    }


    private static boolean loadedRootProjectEnvironment = false;
    /**
     * Transfer the environment into the properties or load for testing from file globalenvironment.env
     * @return true if something was loaded
     */
    public static boolean loadRootProjectEnvironmentVariables()
    {
        if(loadedRootProjectEnvironment)
        {
            return true;
        }
        Properties props = new Properties();
        Path envFile = Paths.get(ROOT_ENVIRONMENT_PATH);
        try (var inputStream = Files.newInputStream(envFile)) {
            props.load(inputStream);
            int c = 0;
            for(Map.Entry<Object,Object> e : props.entrySet())
            {
                System.setProperty(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                c++;
            }

            loadedRootProjectEnvironment = true;
            System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                    "Loading root environment adding " + c + " properties from " + envFile.getFileName().toString());

        }
        catch (IOException ioe)
        {
            System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                    "Environment definition load error: " + ROOT_ENVIRONMENT_PATH +
                            " --> " + envFile.toString() + " " + ioe.getMessage());
        }
        // Overwrite by environment
        int e = 0;
        int o = 0;
        for(Map.Entry<String,String> env :System.getenv().entrySet())
        {
            if(System.getProperty(env.getKey()) != null)
            {
                o++;
            }
            System.setProperty(env.getKey(), env.getValue());
            e++;
        }
        loadedRootProjectEnvironment = loadedRootProjectEnvironment || e > 0;
        System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                "Got " + e + " properties from environment variables overwritten: " + o);
        return loadedRootProjectEnvironment;
    }

    private static void initLogging()
    {
        String mcpGatewayLogging = System.getProperty("MCP_GATEWAY_LOGGING");
        if(mcpGatewayLogging != null) {
            try {
                final Level lvl = Level.parse(mcpGatewayLogging);
                System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO, "MCP_GATEWAY_LOGGING=" + lvl.getName());
                if(lvl == Level.FINER || lvl == Level.ALL || lvl == Level.FINEST)
                {
                    FINER = Level.INFO;
                    FINE = Level.INFO;
                }
                else if(lvl == Level.FINE)
                {
                    FINE = Level.INFO;
                }
                else if(lvl == Level.WARNING || lvl == Level.SEVERE)
                {
                    INFO = Level.FINE;
                }
            }
            catch (IllegalArgumentException ea)
            {
                System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO, "MCP_GATEWAY_LOGGING: " + ea.getMessage());
            }
        }
        else
        {
            System.getLogger(McpGatewayApplication.class.getSimpleName()).log(System.Logger.Level.INFO, "MCP_GATEWAY_LOGGING=(default)");
        }
    }

}
