package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for downloading text files from HTTP/HTTPS URLs
 */
public class FileDownloader {
    
    // Maximum file size: 10MB (to prevent memory issues)
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    /**
     * Downloads a text file from the given URL
     * 
     * @param urlString The URL of the text file to download
     * @return The content of the text file as a String
     * @throws Exception If the download fails (network error, HTTP error, file too large, etc.)
     */
    public static String downloadTextFile(String urlString) throws Exception {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        URL url;
        try {
            url = new URL(urlString);
        } catch (Exception e) {
            throw new Exception("Invalid URL format: " + urlString + " - " + e.getMessage(), e);
        }
        
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            Logger.getLogger().log("Downloading file from URL: " + urlString);
            
            // Open connection
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; AWS-Worker/1.0)");
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error code: " + responseCode + " - " + connection.getResponseMessage());
            }
            
            // Check content length
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE) {
                throw new Exception("File too large: " + contentLength + " bytes (max: " + MAX_FILE_SIZE + " bytes)");
            }
            
            // Read the content
            reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );
            
            StringBuilder content = new StringBuilder();
            String line;
            long totalBytesRead = 0;
            
            while ((line = reader.readLine()) != null) {
                totalBytesRead += line.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for newline
                
                if (totalBytesRead > MAX_FILE_SIZE) {
                    throw new Exception("File too large: exceeds " + MAX_FILE_SIZE + " bytes");
                }
                
                content.append(line).append("\n");
            }
            
            String result = content.toString();
            Logger.getLogger().log("Successfully downloaded file: " + result.length() + " characters from " + urlString);
            
            return result;
            
        } catch (Exception e) {
            Logger.getLogger().log("Error downloading file from " + urlString + ": " + e.getMessage());
            throw e;
        } finally {
            // Clean up resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}

