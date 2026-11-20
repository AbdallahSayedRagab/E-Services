package Utilities;

import org.apache.logging.log4j.LogManager;

// مكان اللوجز بيتحدد من src/main/resources/Log4j2.properties (test-outputs/Logs)
public class LogsUtiles {

    public static void trace (String message){
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString()).trace(message);

    }
    public static void debug (String message){
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString()).debug(message);

    }
    public static void info  (String message){
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString()).info(message);

    }
    public static void warn (String message){
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString()).warn(message);

    }
    public static void error (String message){
        LogManager.getLogger(Thread.currentThread().getStackTrace()[2].toString()).error(message);

    }
}
