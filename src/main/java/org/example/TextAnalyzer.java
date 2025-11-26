package org.example;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

public class TextAnalyzer {
    
    // Singleton instance of LexicalizedParser
    private static LexicalizedParser parser = null;
    private static final String PARSER_MODEL = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    
    /**
     * Initialize the Stanford Parser (lazy initialization)
     */
    private static void initializeParser() {
        if (parser == null) {
            try {
                parser = LexicalizedParser.loadModel(PARSER_MODEL);
                Logger.getLogger().log("Stanford Parser initialized successfully");
            } catch (Exception e) {
                Logger.getLogger().log("Error initializing Stanford Parser: " + e.getMessage());
                throw new RuntimeException("Failed to initialize Stanford Parser", e);
            }
        }
    }
    
    /**
     * Analyze text based on the requested analysis type
     * @param text The text to analyze
     * @param analysisType POS, CONSTITUENCY, or DEPENDENCY
     * @return The analyzed text as a string
     */
    public static String analyze(String text, String analysisType) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        initializeParser();
        
        try {
            switch (analysisType.toUpperCase()) {
                case "POS":
                    return performPOSTagging(text);
                case "CONSTITUENCY":
                    return performConstituencyParsing(text);
                case "DEPENDENCY":
                    return performDependencyParsing(text);
                default:
                    throw new IllegalArgumentException("Unknown analysis type: " + analysisType);
            }
        } catch (Exception e) {
            Logger.getLogger().log("Error during text analysis (" + analysisType + "): " + e.getMessage());
            throw new RuntimeException("Text analysis failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform Part-of-Speech tagging
     * Processes text line by line and tags each word with its POS
     */
    private static String performPOSTagging(String text) {
        StringWriter result = new StringWriter();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                result.write("\n");
                continue;
            }
            
            try {
                // Parse the sentence to get a tree
                Tree parseTree = parser.parse(line);
                
                // Extract POS tags from the tree
                List<TaggedWord> taggedWords = parseTree.taggedYield();
                for (TaggedWord tw : taggedWords) {
                    result.write(tw.word() + "/" + tw.tag() + " ");
                }
                result.write("\n");
            } catch (Exception e) {
                Logger.getLogger().log("Error in POS tagging for line: " + line + " - " + e.getMessage());
                result.write("ERROR: " + e.getMessage() + "\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Perform Constituency parsing
     * Processes text line by line and generates parse trees
     */
    private static String performConstituencyParsing(String text) {
        StringWriter result = new StringWriter();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                result.write("\n");
                continue;
            }
            
            try {
                Tree parseTree = parser.parse(line);
                if (parseTree != null) {
                    result.write(parseTree.toString());
                    result.write("\n");
                }
            } catch (Exception e) {
                Logger.getLogger().log("Error in constituency parsing for line: " + line + " - " + e.getMessage());
                result.write("ERROR: " + e.getMessage() + "\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Perform Dependency parsing
     * Processes text line by line and generates dependency graphs
     */
    private static String performDependencyParsing(String text) {
        StringWriter result = new StringWriter();
        String[] lines = text.split("\n");
        
        PennTreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                result.write("\n");
                continue;
            }
            
            try {
                Tree parseTree = parser.parse(line);
                if (parseTree != null) {
                    GrammaticalStructure gs = gsf.newGrammaticalStructure(parseTree);
                    Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
                    
                    for (TypedDependency td : tdl) {
                        result.write(td.toString());
                        result.write("\n");
                    }
                    result.write("\n");
                }
            } catch (Exception e) {
                Logger.getLogger().log("Error in dependency parsing for line: " + line + " - " + e.getMessage());
                result.write("ERROR: " + e.getMessage() + "\n");
            }
        }
        
        return result.toString();
    }
}
