package net.cytonic.cynturion;

public class CynturionSettings {

    public static String REDIS_HOST;
    public static String REDIS_PASSWORD;
    //rabbitmq
    public static String RABBITMQ_HOST;
    public static String RABBITMQ_PASSWORD;
    public static String RABBITMQ_USERNAME;

    // Database
    /**
     * Should the server use the database
     */
    public static boolean DATABASE_ENABLED = true;
    /**
     * Database username
     */
    public static String DATABASE_USER = "";
    /**
     * Database password
     */
    public static String DATABASE_PASSWORD = "";
    /**
     * Hostname of the database server
     */
    public static String DATABASE_HOST = "";
    /**
     * Database port
     */
    public static int DATABASE_PORT = 3306;
    /**
     * Name of the database to use
     */
    public static String DATABASE_NAME = "";
    /**
     * Use SSL?
     */
    public static boolean DATABASE_USE_SSL = false;


    public static void importFromProperties() {
        if (System.getProperty("REDIS_HOST")!= null) REDIS_HOST = System.getProperty("REDIS_HOST");
        if (System.getProperty("REDIS_PASSWORD")!= null) REDIS_PASSWORD = System.getProperty("REDIS_PASSWORD");
        //rabbitmq
        if (System.getProperty("RABBITMQ_HOST")!= null) RABBITMQ_HOST = System.getProperty("RABBITMQ_HOST");
        if (System.getProperty("RABBITMQ_PASSWORD")!= null) RABBITMQ_PASSWORD = System.getProperty("RABBITMQ_PASSWORD");
        if (System.getProperty("RABBITMQ_USERNAME")!= null) RABBITMQ_USERNAME = System.getProperty("RABBITMQ_USERNAME");

        // database
        if (System.getProperty("DATABASE_USER") != null) DATABASE_USER = System.getProperty("DATABASE_USER");
        if (System.getProperty("DATABASE_PASSWORD") != null)
            DATABASE_PASSWORD = System.getProperty("DATABASE_PASSWORD");
        if (System.getProperty("DATABASE_HOST") != null) DATABASE_HOST = System.getProperty("DATABASE_HOST");
        if (System.getProperty("DATABASE_PORT") != null)
            DATABASE_PORT = Integer.parseInt(System.getProperty("DATABASE_PORT"));
        if (System.getProperty("DATABASE_NAME") != null) DATABASE_NAME = System.getProperty("DATABASE_NAME");
        if (System.getProperty("DATABASE_USE_SSL") != null)
            DATABASE_USE_SSL = Boolean.parseBoolean(System.getProperty("DATABASE_USE_SSL"));
    }

    public static void importFromEnv() {
        if (System.getenv("REDIS_HOST")!= null) REDIS_HOST = System.getenv("REDIS_HOST");
        if (System.getenv("REDIS_PASSWORD")!= null) REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");
        //rabbitmq
        if (System.getenv("RABBITMQ_HOST")!= null) RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
        if (System.getenv("RABBITMQ_PASSWORD")!= null) RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");
        if (System.getenv("RABBITMQ_USERNAME")!= null) RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");

        // database
        if (System.getenv("DATABASE_ENABLED") != null)
            DATABASE_ENABLED = Boolean.parseBoolean(System.getenv("DATABASE_ENABLED"));
        if (System.getenv("DATABASE_USER") != null) DATABASE_USER = System.getenv("DATABASE_USER");
        if (System.getenv("DATABASE_PASSWORD") != null) DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD");
        if (System.getenv("DATABASE_HOST") != null) DATABASE_HOST = System.getenv("DATABASE_HOST");
        if (System.getenv("DATABASE_PORT") != null) DATABASE_PORT = Integer.parseInt(System.getenv("DATABASE_PORT"));
        if (System.getenv("DATABASE_NAME") != null) DATABASE_NAME = System.getenv("DATABASE_NAME");
        if (System.getenv("DATABASE_USE_SSL") != null)
            DATABASE_USE_SSL = Boolean.parseBoolean(System.getenv("DATABASE_USE_SSL"));
    }

}
