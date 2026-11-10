package org.beatengine.mcpserverknowledgebasedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication
public class McpServerKnowledgeBaseDemoApplication {

    // For testing and dev environment is loaded from file
    public static String ROOT_ENVIRONMENT_PATH = new File(Paths.get("").toAbsolutePath().toFile(), "localenvironment.env").getAbsolutePath();

    public static void main(String[] args) {
        //
        loadRootProjectEnvironmentVariables();
        SpringApplication.run(McpServerKnowledgeBaseDemoApplication.class, args);
    }


    private static boolean loadedRootProjectEnvironment = false;

    /**
     * Transfer the environment into the properties or load testing .env
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
            System.getLogger(McpServerKnowledgeBaseDemoApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                    "Loading root environment adding " + c + " properties from " + envFile.getFileName().toString());

        }
        catch (IOException ioe)
        {
            System.getLogger(McpServerKnowledgeBaseDemoApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                    "Environment definition load error: " + ROOT_ENVIRONMENT_PATH +
                            " --> " + envFile.toString() + " " + ioe.getMessage());
        }
        // Overwrite by environment
        int e = 0;
        for(Map.Entry<String,String> env :System.getenv().entrySet())
        {
            System.setProperty(env.getKey(), env.getValue());
            e++;
        }
        loadedRootProjectEnvironment = loadedRootProjectEnvironment || e > 0;
        System.getLogger(McpServerKnowledgeBaseDemoApplication.class.getSimpleName()).log(System.Logger.Level.INFO,
                "Got " + e + " properties from environment variables");
        return loadedRootProjectEnvironment;
    }

}
