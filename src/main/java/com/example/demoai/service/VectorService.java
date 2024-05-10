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

package com.example.demoai.service;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

//CHANGE
//import org.springframework.ai.ollama.OllamaChatClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demoai.controller.DemoaiController;

import com.example.model.JsonDualDTO;

@Service
public class VectorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private volatile VectorStore vectorStore;

    private static final Logger logger = LoggerFactory.getLogger(VectorService.class);

    //CHANGE
    private final ChatClient aiClient;
    //private final OllamaChatClient aiClient;

    //CHANGE
    VectorService(@Qualifier("openAiChatClient") ChatClient aiClient) {
    //VectorService(OllamaChatClient aiClient) {
    
        this.aiClient = aiClient;
    }

    @Value("classpath:prompt-template.txt")
    private Resource templateFile;
 
    private final String templateBasic = """
        DOCUMENTS:
        {documents}
        
        QUESTION:
        {question}
        
        INSTRUCTIONS:
        Answer the users question using the DOCUMENTS text above.
        Keep your answer ground in the facts of the DOCUMENTS.
        If the DOCUMENTS doesn’t contain the facts to answer the QUESTION, return: 
        I'm sorry but I haven't enough information to answer.
        """;

   
    public VectorStore getVectorStore() {
        return vectorStore;
    }
    
    //EXPERIMENTAL - NOT FULLY TESTED
    public Boolean putJsonView(String jsonView) {

        String sqlStatement = "select DATA from " + jsonView;

        RowMapper<JsonDualDTO> rowMapper = new RowMapper<JsonDualDTO>() {
            @Override
            public JsonDualDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                JsonDualDTO jsonElem = new JsonDualDTO();

                String data = rs.getString(1);
                jsonElem.setDATA(data);
                return jsonElem;
            }
        };
        try {
            List<JsonDualDTO> ret = jdbcTemplate.query(sqlStatement, rowMapper);

            for (JsonDualDTO j : ret) {
                Resource resourceJsonDual = new ByteArrayResource(("[" + j.getDATA() + "]").getBytes());
                JsonReader jsonReader = new JsonReader(resourceJsonDual);
                var textSplitter = new TokenTextSplitter();
                this.vectorStore.accept(textSplitter.apply(jsonReader.get()));
            }
            logger.info("JSON view: " + jsonView + " stored as embedding");
            return true;
        } catch (Exception ex) {
            logger.error(sqlStatement);
            return false;
        }
    }

  
    public void putDocument(Resource docResource) {
        
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(docResource,
				PdfDocumentReaderConfig.builder()
					.withPageTopMargin(0)
					.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfTopTextLinesToDelete(0)
						.build())
					.withPagesPerDocument(1)
					.build());

        var textSplitter = new TokenTextSplitter();
        this.vectorStore.accept(textSplitter.apply(pdfReader.get()));
    }
 

    public String rag(String question) {

        String START = "\n<article>\n";
        String STOP = "\n</article>\n";
        
        List<Document> similarDocuments = this.vectorStore.similaritySearch( 
            SearchRequest.
               query(question).
               withTopK(4));

        Iterator<Document> iterator = similarDocuments.iterator();
        StringBuilder context = new StringBuilder();
        while (iterator.hasNext()) {
            Document document = iterator.next();
            context.append(document.getId() + ".");
            context.append(START + document.getFormattedContent() + STOP);
        }

        String template = templateBasic;
        try {
            template = new String(Files.readAllBytes(templateFile.getFile().toPath()), StandardCharsets.UTF_8);

        } catch (IOException e) {
            logger.error(e.getMessage());
            template = templateBasic;
        }
        ;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of("documents", context, "question", question));
        logger.info(prompt.toString());
        ChatResponse aiResponse = aiClient.call(prompt);
        return aiResponse.getResult().getOutput().getContent();
    }

    public List<Document> getSimilarDocs(String message) {
        List<Document> similarDocuments = this.vectorStore.similaritySearch(message);
        return similarDocuments;

    }

}
