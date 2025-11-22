package org.example;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.File;
import java.util.List;
import java.util.Map;

import static java.lang.System.exit;
import static org.example.SqsService.MANAGER_TO_WORKER_REQUEST_QUEUE;

public class WorkerApp {



    public static void run(String[] args){
        //in the future, we might pass args to worker app
        Map<String, String> terminalParamsMap = parseArgs(args);

        //check every second if the result file is in S3 by looking for a "Done" message in the SQS
        while (!done()){

            List<Message> messages = SqsService.getMessagesForQueue(MANAGER_TO_WORKER_REQUEST_QUEUE);
            if (!messages.isEmpty()){
                for (Message message : messages) {
                    //first delete message to avoid double processing in case of errors
                    SqsService.deleteMessage(MANAGER_TO_WORKER_REQUEST_QUEUE, message);
                    Logger.getLogger().log("Received message: " + message.body());
                    handleWorkerMessage(message);
                }
            }
        }


    }

    private static void handleWorkerMessage(Message message) {
        //TODO: implement worker message handling logic
    }

    private static Map<String, String> parseArgs(String[] args){

        Map<String, String> params = new java.util.HashMap<>();
        for (String arg : args){
            params.put("some_param_key", arg); //replace with actual parsing logic
        }
        return params;
    }


    enum AnalysisTypes {
        POS, CONSTITUENCY, DEPENDENCY
    }

    enum TerminalParams {
        FILE_PATH, OUTPUT_PATH, FILE_CAP, TERMINATE
    }
    
    static  AnalysisTypes getAnalysisType(String analysisType){
        switch (analysisType.toLowerCase()){
            case "pos":
                return AnalysisTypes.POS;
            case "constituency":
                return AnalysisTypes.CONSTITUENCY;
            case "dependency":
                return AnalysisTypes.DEPENDENCY;
            default:
                return null;
        }
    }

    static boolean done(){
        //later we can determine if we are done based on some condition like is file done processing
        return false;
    }


    enum taskTypes{
        DONE, ERROR
    }

}
