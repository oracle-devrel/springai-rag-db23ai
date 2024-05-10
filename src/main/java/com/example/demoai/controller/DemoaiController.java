// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

// ORACLE AND ITS AFFILIATES DO NOT PROVIDE ANY WARRANTY WHATSOEVER, EXPRESS OR IMPLIED, 
// FOR ANY SOFTWARE, MATERIAL OR CONTENT OF ANY KIND CONTAINED OR PRODUCED WITHIN THIS REPOSITORY,
// AND IN PARTICULAR SPECIFICALLY DISCLAIM ANY AND ALL IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT,
// MERCHANTABILITY, AND FITNESS FOR A PARTICULAR PURPOSE. FURTHERMORE, ORACLE AND ITS AFFILIATES 
// DO NOT REPRESENT THAT ANY CUSTOMARY SECURITY REVIEW HAS BEEN PERFORMED WITH RESPECT TO ANY SOFTWARE,
// MATERIAL OR CONTENT CONTAINED OR PRODUCED WITHIN THIS REPOSITORY. IN ADDITION, AND WITHOUT LIMITING 
// THE FOREGOING, THIRD PARTIES MAY HAVE POSTED SOFTWARE, MATERIAL OR CONTENT TO THIS REPOSITORY WITHOUT
// ANY REVIEW. USE AT YOUR OWN RISK.

package com.example.demoai.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//CHANGE
//import org.springframework.ai.ollama.OllamaEmbeddingClient;
//import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.ChatClient;

import com.example.demoai.service.VectorService;
import com.example.model.MessageDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("ai")
public class DemoaiController {

    //CHANGE
    private final EmbeddingClient embeddingClient;
    //private final OllamaEmbeddingClient embeddingClient;

    //CHANGE
    private final ChatClient chatClient;
    //private final OllamaChatClient chatClient;


    private final VectorService vectorService;

    @Value("${config.tempDir}")
    private String TEMP_DIR;

    private static final Logger logger = LoggerFactory.getLogger(DemoaiController.class);

    //CHANGE
    @Autowired
    public DemoaiController(EmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // OpenAI full
    //public DemoaiController(OllamaEmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // Ollama Embeddings - OpenAI Completion 
    //public DemoaiController(OllamaEmbeddingClient embeddingClient, OllamaChatClient chatClient, VectorService vectorService) { // Ollama full 
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
        this.vectorService = vectorService;

        logger.info("Controller started.");
    }

    @GetMapping("/ping")
    public String ping(@RequestParam(value = "message", defaultValue = "ping success") String message) {

        return "Hello: " + message;
    }

    /**
     * Call OpenAI Chat completion
     * 
     * @param message query in natural language
     * @return generation
     */
    @GetMapping("/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "Give me a trading strategy") String message) {
        return Map.of("generation", chatClient.call(message));
    }

    /**
     * Call OpenAI Chat completion
     * 
     * @RequestBody MessageDTO message query in natural language in the request body
     * @return generation
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody MessageDTO message) {
        return Map.of("generation", chatClient.call(message.getMessage()));
    }

    /**
     * RAG
     * 
     * @param message query in natural language
     * @return generation
     */
    @GetMapping("/rag")
    public Map rag(@RequestParam(value = "message", defaultValue = "Give me a trading strategy") String message) {
        return Map.of("generation", this.vectorService.rag(message));
    }

    /**
     * RAG
     * 
     * @RequestBody MessageDTO message query in natural language in the request body
     * @return generation
     */
    @PostMapping("/rag")
    public Map<String, Object> rag(@RequestBody MessageDTO message) {

        return Map.of("generation", this.vectorService.rag(message.getMessage()));
    }

    @GetMapping("/embedding")
    public Map embed(@RequestParam(value = "message", defaultValue = "Give me a trading strategy") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingClient.embedForResponse(List.of(message));
        return Map.of("embedding", embeddingResponse);
    }

    @PostMapping("/embedding")
    public Map<String, Object> embed(@RequestBody MessageDTO message) {
        EmbeddingResponse embeddingResponse = this.embeddingClient.embedForResponse(List.of(message.getMessage()));
        return Map.of("embedding", embeddingResponse);
    }

    /**
     * Store a pdf file in Oracle Vector DB 23c
     * 
     * @param file pdf file sent as Multipart
     * @return success/fail message
     */
    @PostMapping("/store")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Failed to upload empty file.";
        }
        try {
            String currentDir = System.getProperty("user.dir");
            Path parentDir = Path.of(currentDir);
            Path tempPath = parentDir.resolve(TEMP_DIR);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
            }
            Path tempDir = Files.createTempDirectory(tempPath, "uploads_");
            Path filePath = tempDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            this.vectorService.putDocument(new FileSystemResource(filePath.toString()));

            return "File stored successfully " + filePath;
        } catch (IOException e) {
            logger.info(e.getMessage());
            return "Failed to upload file: " + e.getMessage();
        }
    }

    @GetMapping("/delete")
    public Map delete(@RequestParam(value = "id", defaultValue = "XXXXXXXXX") List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of("return", "No IDs provided");
        }
        Optional<Boolean> ret = vectorService.getVectorStore().delete(ids);
        return Map.of("return", ret.get().toString());
    }

    @PostMapping("/search-similar")
    public List<Map<String, Object>> search(@RequestBody MessageDTO message) {
    
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<Document> similarDocs = this.vectorService.getSimilarDocs(message.getMessage());

        for (Document d : similarDocs) {
            Map<String, Object> metadata = d.getMetadata();
            Map doc = new HashMap<>();
            doc.put("id", d.getId());
            doc.put("text", d.getContent());
            doc.put("metadata", metadata);
            resultList.add(doc);
        }
        ;
        return resultList;
    }

    /**
     * Store a Json Duality view as documents: EXPERIMENTAL - NOT FULLY TESTED
     * 
     * @param view the name of Json-Duality view already created in the DB
     * @return success/fail message
     */
    @GetMapping("/store-json")
    public String storeJson(@RequestParam("view") String viewName) {
        if (viewName.isEmpty()) {
            return "view empty";
        }

        Boolean retMessage = this.vectorService.putJsonView(viewName);
        if (retMessage) {
            return "SUCCESS: Json Duality View stored as embedding ";
        } else {
            return "ERROR: Failed to store Json Duality View as embedding!";
        }

    }

}
