package org.example;

public class Logger {
    static Logger instance;
    public static Logger getLogger(){
        if (instance == null){
            instance = new Logger();
        }
        return instance;
    }
    public void log(String message){
        System.out.println("WORKER-LOG: " + getNiceTime(System.currentTimeMillis()) + "    " + message);
        try {
            // Only send to SQS if SqsService is available (avoid classloader issues during testing)
            SqsService.sendMessage(SqsService.LOG_TO_LOCAL, "WORKER-LOG: " + getNiceTime(System.currentTimeMillis()) + "    " + message);
        } catch (Exception | LinkageError e) {
            // Silently ignore SQS errors and classloader issues (Java 11+ compatibility)
            // This allows the application to continue even if SQS logging fails
        }
    }

    static String getNiceTime(long millis){
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
    }
}
