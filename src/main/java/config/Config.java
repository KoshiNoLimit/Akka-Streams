package config;

import java.time.Duration;

public class Config {
    public static final String SYSTEM_NAME = "routes";
    public static final String HOST = "localhost";
    public static final Integer PORT = 8080;
    public static final String ON_START = "start!";
    public static final String SERVER_START_MESSAGE = "Server online at http://" + HOST + ":" + PORT + "\nPress RETURN to stop...";
    public static final String ULR_PARAMETER = "testUrl";
    public static final String COUNT_PARAMETER = "count";
    public static final Integer MAX_STREAMS = 4;
    public static final Duration TIMEOUT = Duration.ofMillis(5000);
    public static final String  HAVENT = "Have not";
    public static final Integer NANO_SIZE = 1000000;

}
