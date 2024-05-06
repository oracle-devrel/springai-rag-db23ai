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

package com.example.demoai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import com.example.model.VectorData;


//Oracle DB
import oracle.jdbc.OracleType;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;


import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleDBVectorStore implements VectorStore, InitializingBean {

    private static final List<String> DISTANCE_METRICS = Collections.unmodifiableList(
            List.of("MANHATTAN", "EUCLIDEAN", "DOT", "COSINE"));

    private Map<String, String> DISTANCE_METRICS_FUNC;

    @Value("${config.vectorDB:vectortab}")
    public String VECTOR_TABLE;

    public int BATCH_SIZE = 100;

    private JdbcTemplate jdbcTemplate;

    EmbeddingClient embeddingClient;

    @Value("${config.dropDb}")
    private Boolean dropAtStartup;

    @Value("${config.distance}")
    private String distance_metric = "COSINE";

    private static final Logger logger = LoggerFactory.getLogger(OracleDBVectorStore.class);

    public OracleDBVectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embClient) {

        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embClient;
        this.DISTANCE_METRICS_FUNC = new HashMap<>();
        this.DISTANCE_METRICS_FUNC.put("MANHATTAN", "L1_DISTANCE");
        this.DISTANCE_METRICS_FUNC.put("EUCLIDEAN", "L2_DISTANCE");
        this.DISTANCE_METRICS_FUNC.put("DOT", "INNER_PRODUCT");
        this.DISTANCE_METRICS_FUNC.put("COSINE", "COSINE_DISTANCE");
    }

    @Override
    public void add(List<Document> documents) {

        int size = documents.size();

        this.jdbcTemplate.batchUpdate("INSERT INTO " + this.VECTOR_TABLE + " (text,embeddings,metadata) VALUES (?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {

                        var document = documents.get(i);
                        var text = document.getContent();
                        
                        OracleJsonFactory factory = new OracleJsonFactory();
                        OracleJsonObject jsonObj = factory.createObject();
                        Map<String, Object> metaData = document.getMetadata();
                        for (Map.Entry<String, Object> entry : metaData.entrySet()) {
                            jsonObj.put(entry.getKey(), String.valueOf(entry.getValue()));
                        }

                        List<Double> vectorList = embeddingClient.embed(document);
                        double[] embeddings = new double[vectorList.size()];
                        for (int j = 0; j < vectorList.size(); j++) {
                            embeddings[j] = vectorList.get(j);
                        }
     
                        ps.setString(1, text);
                        ps.setObject(2, embeddings, OracleType.VECTOR);
                        ps.setObject(3, jsonObj, OracleType.JSON);

                    }

                    @Override
                    public int getBatchSize() {
                        return size;
                    }
                });

    }


    @Override
    public Optional<Boolean> delete(List<String> idList) {

        String sql = "DELETE FROM " + this.VECTOR_TABLE + " WHERE id = ?";
        int count[][] = jdbcTemplate.batchUpdate(sql, idList, BATCH_SIZE, (ps, argument) -> {
            ps.setString(1, argument);
        });

        int sum = Arrays.stream(count).flatMapToInt(Arrays::stream).sum();
        logger.info("MSG: Deleted " + sum + " records");

        return Optional.of(sum == idList.size());
    }


    @Override
    public List<Document> similaritySearch(SearchRequest request) {

        List<VectorData> nearest = new ArrayList<>();

        logger.info("MSG: REQUESTED QUERY " + request.getQuery());
        List<Double> queryEmbeddings = embeddingClient.embed(request.getQuery());
        logger.info("MSG: EMBEDDINGS SIZE: " + queryEmbeddings.size());

        logger.info("MSG: DISTANCE METRICS: " + distance_metric);
        if (DISTANCE_METRICS_FUNC.get(distance_metric) == null) {
            logger.error(
                    "ERROR: wrong distance metrics set. Allowed values are: " + String.join(",", DISTANCE_METRICS));
            System.exit(1);
        }
        logger.info("MSG: DISTANCE METRICS FUNCTION: " + DISTANCE_METRICS_FUNC.get(distance_metric));
        int topK = request.getTopK();

        try {
            nearest = similaritySearchByMetrics(VECTOR_TABLE, queryEmbeddings, topK,
                    this.DISTANCE_METRICS_FUNC.get(distance_metric));
        } catch (Exception e) {
            logger.error(e.toString());
        }

        List<Document> documents = new ArrayList<>();

        for (VectorData d : nearest) {
            OracleJsonObject metadata = d.getMetadata();
            Map<String, Object> map = new HashMap<>();
            for (String key : metadata.keySet()) {
                map.put(key, metadata.get(key).toString());
            }
            Document doc = new Document(d.getText(), map);
            documents.add(doc);

        }
        return documents;

    }

    List<VectorData> similaritySearchByMetrics(String vectortab, List<Double> vector, int topK,
            String distance_metrics_func) throws SQLException {
        List<VectorData> results = new ArrayList<>();
        double[] doubleVector = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            doubleVector[i] = vector.get(i);
        }

        try {

            String similaritySql = "SELECT id,embeddings,metadata,text FROM " + vectortab
                    + " ORDER BY " + distance_metrics_func + "(embeddings, ?)"
                    + " FETCH FIRST ? ROWS ONLY";

            results = jdbcTemplate.query(similaritySql,
                    new PreparedStatementSetter() {
                        public void setValues(java.sql.PreparedStatement ps) throws SQLException {
                            ps.setObject(1, doubleVector, OracleType.VECTOR);
                            ps.setObject(2, topK, OracleType.NUMBER);
                        }
                    },
                    new RowMapper<VectorData>() {
                        public VectorData mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return new VectorData(rs.getString("id"),
                                    rs.getObject("embeddings", double[].class),
                                    rs.getObject("text", String.class),
                                    rs.getObject("metadata", OracleJsonObject.class));
                        }
                    });

        } catch (Exception e) {
            logger.error("ERROR: " + e.getMessage());
        }
        return results;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        int initialSize = 0;
        try {

            initialSize = jdbcTemplate.queryForObject("select count(*) from " + this.VECTOR_TABLE, Integer.class);
            logger.info("MSG: table " + this.VECTOR_TABLE + " exists with " + initialSize + " chunks");

            if (dropAtStartup) {
                logger.info("MSG: DROP TABLE " + this.VECTOR_TABLE + " AT EVERY STARTUP");
                throw new Exception("DROP TABLE EVERY STARTUP");
            }
        } catch (Exception e) {
            try {
                logger.info("MSG: DROPPING TABLE " + this.VECTOR_TABLE);
                jdbcTemplate.execute(
                        "BEGIN\n" +
                                "   EXECUTE IMMEDIATE 'DROP TABLE " + this.VECTOR_TABLE + " CASCADE CONSTRAINTS';\n" +
                                "EXCEPTION\n" +
                                "   WHEN OTHERS THEN\n" +
                                "       IF SQLCODE != -942 THEN\n" +
                                "           RAISE;\n" +
                                "       END IF;\n" +
                                "END;");
            } catch (Exception ex1) {
                logger.error("ERROR: DROP TABLE " + this.VECTOR_TABLE + " \n" + e.getMessage());
                System.exit(1);
            }
            String createTableSql = "CREATE TABLE " + this.VECTOR_TABLE + "(" +
                    "id NUMBER GENERATED AS IDENTITY ," +
                    "text CLOB," +
                    "embeddings VECTOR," +
                    "metadata JSON," +
                    "PRIMARY KEY (id))";
            try {
                this.jdbcTemplate.execute(
                        "BEGIN\n" +
                                "   EXECUTE IMMEDIATE '" +
                                createTableSql + "' ;\n" +
                                "EXCEPTION\n" +
                                "   WHEN OTHERS THEN\n" +
                                "       IF SQLCODE != -942 THEN\n" +
                                "           RAISE;\n" +
                                "       END IF;\n" +
                                "END;");
                logger.info("OK: CREATE TABLE " + this.VECTOR_TABLE);
            } catch (Exception ex2) {
                logger.error("ERROR: CREATE TABLE" + e.getMessage());
                System.exit(1);
            }
        }
        return;
    }

}
