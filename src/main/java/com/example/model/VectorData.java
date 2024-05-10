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

package com.example.model;

import oracle.sql.json.OracleJsonObject;

public class VectorData {
    private String id;
    private double[]  embeddings; 
    private String text ;
    private OracleJsonObject metadata;
    

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public OracleJsonObject getMetadata() {
        return metadata;
    }

    public void setMetadata(OracleJsonObject metadata) {
        this.metadata = metadata;
    }

    // Constructor
    public VectorData(String id, double[] embeddings, String text, OracleJsonObject metadata) {
        this.id = id;
        this.embeddings = embeddings;
        this.metadata = metadata;
        this.text = text;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double[] getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(double[] embeddings) {
        this.embeddings = embeddings;
    }
}
