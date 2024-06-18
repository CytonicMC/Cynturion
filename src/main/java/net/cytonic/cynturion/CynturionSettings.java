package net.cytonic.cynturion;

public class CynturionSettings {

    public static String REDIS_HOST;
    public static String REDIS_PASSWORD;
    //rabbitmq
    public static String RABBITMQ_HOST;
    public static String RABBITMQ_PASSWORD;
    public static String RABBITMQ_USERNAME;

    public static void importFromProperties() {
        if (System.getProperty("REDIS_HOST")!= null) REDIS_HOST = System.getProperty("REDIS_HOST");
        if (System.getProperty("REDIS_PASSWORD")!= null) REDIS_PASSWORD = System.getProperty("REDIS_PASSWORD");
        //rabbitmq
        if (System.getProperty("RABBITMQ_HOST")!= null) RABBITMQ_HOST = System.getProperty("RABBITMQ_HOST");
        if (System.getProperty("RABBITMQ_PASSWORD")!= null) RABBITMQ_PASSWORD = System.getProperty("RABBITMQ_PASSWORD");
        if (System.getProperty("RABBITMQ_USERNAME")!= null) RABBITMQ_USERNAME = System.getProperty("RABBITMQ_USERNAME");
    }

    public static void importFromEnv() {
        if (System.getenv("REDIS_HOST")!= null) REDIS_HOST = System.getenv("REDIS_HOST");
        if (System.getenv("REDIS_PASSWORD")!= null) REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");
        //rabbitmq
        if (System.getenv("RABBITMQ_HOST")!= null) RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
        if (System.getenv("RABBITMQ_PASSWORD")!= null) RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");
        if (System.getenv("RABBITMQ_USERNAME")!= null) RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");
    }

}
