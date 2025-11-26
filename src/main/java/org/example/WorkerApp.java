package org.example;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.File;
import java.util.List;
import java.util.Map;

import static java.lang.System.exit;
import static org.example.SqsService.MANAGER_TO_WORKER_REQUEST_QUEUE;
import static org.example.SqsService.WORKER_TO_MANAGER_REQUEST_QUEUE;

public class WorkerApp {



    public static void run(String[] args){
        //in the future, we might pass args to worker app
//        Map<String, String> terminalParamsMap = parseArgs(args);

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

    // Made package-private for testing
    static void handleWorkerMessage(Message message) {
        String messageBody = message.body();
        Logger.getLogger().log("Processing message: " + messageBody);

        // Parse message: format is "ANALYSIS_TYPE\tURL" (tab-separated)
        String[] parts = messageBody.split("\t");
        if (parts.length != 2) {
            Logger.getLogger().log("Invalid message format. Expected: ANALYSIS_TYPE\\tURL, got: " + messageBody);
            SqsService.sendMessage(WORKER_TO_MANAGER_REQUEST_QUEUE,
                "ERROR;UNKNOWN;" + messageBody + ";Invalid message format");
            return;
        }

        String analysisType = parts[0].trim();
        String inputUrl = parts[1].trim();

        Logger.getLogger().log("Analysis Type: " + analysisType + ", URL: " + inputUrl);

        try {
            // Step 1: Download the text file from URL
            Logger.getLogger().log("Downloading text file from: " + inputUrl);
            String textContent = FileDownloader.downloadTextFile(inputUrl);

            if (textContent == null || textContent.isEmpty()) {
                throw new Exception("Downloaded file is empty");
            }

            Logger.getLogger().log("Downloaded " + textContent.length() + " characters");

            // Step 2: Perform the requested analysis
            Logger.getLogger().log("Performing " + analysisType + " analysis...");
            String analysisResult = TextAnalyzer.analyze(textContent, analysisType);

            if (analysisResult == null || analysisResult.isEmpty()) {
                throw new Exception("Analysis produced empty result");
            }

            Logger.getLogger().log("Analysis complete. Result length: " + analysisResult.length() + " characters");

            // Step 3: Upload the analysis result to S3
            Logger.getLogger().log("Uploading analysis result to S3...");
            String s3Url = S3Service.uploadAnalysisResult(analysisResult, analysisType, inputUrl);

            if (s3Url == null) {
                throw new Exception("Failed to upload result to S3");
            }

            Logger.getLogger().log("Upload successful. S3 URL: " + s3Url);

            // Step 4: Send completion message to Manager
            // Format: "ANALYSIS_TYPE;INPUT_URL;OUTPUT_S3_URL"
            String completionMessage = analysisType + ";" + inputUrl + ";" + s3Url;
            SqsService.sendMessage(WORKER_TO_MANAGER_REQUEST_QUEUE, completionMessage);
            Logger.getLogger().log("Sent completion message to manager: " + completionMessage);

        } catch (Exception e) {
            // Handle errors: send error message to Manager
            // Format: "ERROR;ANALYSIS_TYPE;INPUT_URL;ERROR_MESSAGE"
            String errorMessage = "ERROR;" + analysisType + ";" + inputUrl + ";" + e.getMessage();
            Logger.getLogger().log("Error processing message: " + e.getMessage());
            Logger.getLogger().log("Sending error message to manager: " + errorMessage);
            SqsService.sendMessage(WORKER_TO_MANAGER_REQUEST_QUEUE, errorMessage);
        }
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
