package org.example;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.File;

public class S3Service {

    //static instance
    static S3Service instance;

    //static String bucketName = "yehuda-awsremote-20251113";
    static String bucketName = "aws-bucket-worker-project";


    static S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();

    public static void handleCompletion(String fileName, Message message){
        S3Client s3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();

        String key = message.body().split(";")[1] + "_output.txt";
        File outputFile = new File("output_" + fileName);
        try {
            s3.getObject(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    outputFile.toPath());
            Logger.getLogger().log("Output file downloaded: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.getLogger().log("Error downloading output file: " + e.getMessage());
        }
    }

    //upload file to S3
    public static String uploadFile(File file){
        String key = file.getName();
        try {
            //i manually created this bucket in AWS console
            s3.putObject(builder -> builder.bucket(bucketName).key(key).build(),
                    file.toPath());
            Logger.getLogger().log("File uploaded to S3: " + key);
            return "s3://" + bucketName + "/" + key;
        } catch (Exception e) {
            Logger.getLogger().log("Error uploading file to S3: " + e.getMessage());
            return null;
        }
    }

    //use uploadFile to upload analysis result via a temp file with unique name
    public static String uploadAnalysisResult(String content, String analysisType, String inputUrl) {
        // Generate a safe filename by extracting meaningful parts from URL
        String urlSafeName = generateSafeFilename(inputUrl);
        String filename = analysisType + "_" + urlSafeName + "_" + System.currentTimeMillis() + ".txt";

        File file = null;
        try {
            // Create file with meaningful name in system temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            file = new File(tempDir, filename);

            // Write content to file
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            Logger.getLogger().log("Created temp file: " + file.getAbsolutePath());

            // Upload using existing method (it will use filename as S3 key)
            String s3Url = uploadFile(file);

            // Clean up: delete temp file
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Logger.getLogger().log("Deleted temp file: " + file.getAbsolutePath());
                } else {
                    Logger.getLogger().log("Warning: Could not delete temp file: " + file.getAbsolutePath());
                }
            }

            return s3Url;

        } catch (Exception e) {
            Logger.getLogger().log("Error in uploadAnalysisResult: " + e.getMessage());

            // Clean up on error
            if (file != null && file.exists()) {
                try {
                    file.delete();
                } catch (Exception deleteEx) {
                    // Ignore delete errors
                }
            }
            return null;
        }
    }

    /**
     * Generate a safe filename from URL by extracting the last meaningful part
     * Example: https://www.gutenberg.org/files/1659/1659-0.txt -> 1659-0-txt
     */
    private static String generateSafeFilename(String url) {
        try {
            // Extract filename from URL
            String filename = url.substring(url.lastIndexOf('/') + 1);

            // Remove extension and replace dots/dashes with underscores
            if (filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf('.'));
            }

            // Replace any remaining invalid characters
            filename = filename.replaceAll("[^a-zA-Z0-9_-]", "_");

            // If empty or too long, use hash
            if (filename.isEmpty() || filename.length() > 50) {
                filename = String.valueOf(url.hashCode()).replace("-", "n");
            }

            return filename;
        } catch (Exception e) {
            // Fallback: use hash of URL
            return String.valueOf(url.hashCode()).replace("-", "n");
        }
    }

}
