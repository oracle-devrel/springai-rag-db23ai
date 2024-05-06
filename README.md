# Spring AI for RAG on Oracle 23ai Vector DB with OpenAI and private LLMs

![cover](./img/cover.png)

## Introduction

In this demo, we'll guide you through the process of leveraging Java, Spring Boot, Oracle DB23ai and the innovative Spring AI APIs to create next-generation applications.

- Build a Spring Boot Application with RAG (Retrieval Augmented Generation): Discover how to leverage Spring AI to implement a knowledge management system that retrieves relevant information and utilizes large language models to generate insightful responses.
- Integrate Domain Knowledge from Oracle 23ai: Learn how to connect your Spring Boot application with Oracle's 23ai to access and utilize domain-specific knowledge for more accurate and relevant responses.
- Transition to Production with Oracle Backend Platform: We'll address the challenges of moving your knowledge management system from development to production using the Oracle Backend Platform for Spring Boot and Microservices.

Check out [demo here](https://youtu.be/XXXXXXX)

The demo shows a Retrieval-Augmented Generation using the following modules:

    * Spring AI API
    * Oracle DB 23ai
    * OpenAI Embeddings
    * OpenAI Chat
    * OLLAMA local LLM embeddings model
    * OLLAMA local LLM LLama2 model for chat

## Description

This demo is based on a early draft example of **Spring AI API** implementation for the **Oracle 23ai** as vector store, according to the specifications reported here: **[Vector DBs](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)**.

The interface, that must be implemented to use Oracle 23ai as a Vector Store in a Spring AI pipeline, is the following:

    ```java
    public interface VectorStore {

        void add(List<Document> documents);

        Optional<Boolean> delete(List<String> idList);

        List<Document> similaritySearch(SearchRequest request);

        List<Document> similaritySearch(String query);
    }
    ```

which addresses the uploading of documents into a generic Vector DB augmenting with related vector embeddings, and it allows searching for similar documents using the vector distance chosen. All interfaces have been implemented except the `similaritySearch(String query)`, since in the interface it already uses the `similaritySearch(SearchRequest request)` as shown here:

    ```java
    default List<Document> similaritySearch(String query) {
            return this.similaritySearch(SearchRequest.query(query));
        }
    ```

The file `src/main/java/com/example/demoai/OracleDBVectorStore.java` holds the implementation.

The Vector Store implementation for the Oracle DB 23ai saves the data in this **VECTORTABLE**:

    ```sql
    CREATE TABLE VECTORTAB (
            id NUMBER GENERATED AS IDENTITY,
            text CLOB,
            embeddings VECTOR,
            metadata JSON,
            PRIMARY KEY (id)
        );
    ```

The **id** will be based on an generated **Identity** Column key, but you can change it easly in the implementation.

The metadata content depends on what's coming from Document object, and in this case it will hold the following data:

    ```json
    {
        "page_number":"xxx",
        "file_name":"xxx", 
    }
    ```

This table is created at each application startup by default but, configuring the `config.dropDb` parameter to `false` in the `application-dev.properties` you can cumulate at each startup, in the same vector tab, several documents to increase your knowledge base on which run your search.

In terms of source documents on which run Retrieval-Augmented Generation, i this demo are shown two different type:

- **PDF** file splitted in chuncks and stored with vector embeddings
- **JSON** docs created exploiting **JSON-Duality** on existing tables

In term of endpoint services, that you can see in the [DemoaiController.java](src/main/java/com/example/demoai/controller/DemoaiController.java), the following main REST services have been implemented:

- **/store**

    Accepts a PDF doc to be chuncked, created vector embeddings, stored in the **VECTORTABLE**.

- **/store-json**

    Providing the name of a **relational duality view** created on the DB, this service create for each json record a vector embeddings and store it as a common chunks in the **VECTORTABLE**. This service show as you can involve in the RAG, not only unstructered text data coming from documents, but also existing table that will queried in natural language as JSON view.

- **/rag**
    Providing a query in natural language, it will be managed in a Retrieval-Augmented Generation pipeline that will use the content of **VECTORTABLE**, adding the most similar chunks to the question to the context and sending everything using a template in the file: [prompt-template.txt](src/main/resources/prompt-template.txt)

As test only have been also implemented:

- **/search-similar**

    Return a list of the nearest chuncks to the message provided that are stored in the **VECTORTABLE**. It's useful to get info about the context used to determine the prompt sent to the LLM for the completion process and use as references to provide a response.

- **/delete**

    remove from **VECTORTABLE** a list of chunks identified by their IDs

- **/embedding**

    Provide a vector embeddings based on the input string

- **/generate**

    Chat client that doesn't use the RAG pipeline. It could be used as a baseline to show the differences between a response provided by the LLM service as-is (OpenAI, OLLAMA) and an augmented request. It's useful to check if a public content has been used for LLM training, if the response is near what do you expect, without providing your documents.

### Prerequisites and setup

#### JDBC driver for Oracle DB 23ai

This demo works with the latest `ojdbc11.jar` driver related to the Oracle DBMS 23.4.0.0. To run this project, download this driver from Oracle site or directly from your DB server, looking in the directory: `$ORACLE_HOME/jdbc/lib/ojdbc11.jar`. After downloading in your local home dir, import as local MVN artifact with this command:

    ```bash
    mvn install:install-file -Dfile=<HOME_DIR>/ojdbc11.jar -DgroupId=com.oracle.database.jdbc -DartifactId=ojdbc11 -Dversion=23.4.0.0 -Dpackaging=jar -DgeneratePom=true
    ```

#### Environment variables

Set in the environment variables in a `env.sh` file with this content, according your server IPs:

    ```bash
    export OPENAI_URL=https://api.openai.com
    export OPENAI_MODEL=gpt-3.5-turbo
    export OPENAI_EMBEDDING_MODEL=text-embedding-ada-002
    export VECTORDB=[VECTORDB_IP]
    export DB_USER=vector
    export DB_PASSWORD=vector
    export OLLAMA_URL=http://[GPU_SERVER_IP]:11434
    export OLLAMA_EMBEDDINGS=NousResearch--llama-2-7b-chat-hf
    export OLLAMA_MODEL=llama2:7b-chat-fp16
    export OPENAI_API_KEY=[YOUR_OPENAI_KEY]
    #export OPENAI_URL=http://[GPU_SERVER_IP]:3000
    #export OPENAI_MODEL=NousResearch--llama-2-7b-chat-hf
    ```

To invoke both OpenAI `gpt-3.5-turbo` and `text-embedding-ada-002` you need an `YOUR_OPENAI_KEY` that it must relesead by the [Open AI developer platform](https://platform.openai.com/).

About the OLLAMA_EMBEDDINGS/MODEL used, you are free for your experiment to go on the [OLLAMA Library](https://ollama.com/library) and choose other models.

As you can see, you can configure also the `OPENAI_URL`, that help to target an OpenAI LLM providers compatible with the OpenAI APIs. In this way you can switch easly to other providers, even private. In this current release hasn't been implemented, but in future maybe.

Set env with command in a shell:

    ```bash 
    source ./env.sh
    ```

#### DB23ai setup

Follow setup instruction in doc [TBD]:

```
Oracle 23ai Getting Started
```

For JSON-Duality search, create the following table/view according the [oracle-base example](https://oracle-base.com/articles/23c/json-relational-duality-views-23c). As user **vector/vector**:

    ```sql
    drop table if exists emp purge;
    drop table if exists dept purge;

    create table dept (
    deptno number(2) constraint pk_dept primary key,
    dname varchar2(14),
    loc varchar2(13)
    ) ;

    create table emp (
    empno number(4) constraint pk_emp primary key,
    ename varchar2(10),
    job varchar2(9),
    mgr number(4),
    hiredate date,
    sal number(7,2),
    comm number(7,2),
    deptno number(2) constraint fk_deptno references dept
    );

    create index emp_dept_fk_i on emp(deptno);

    insert into dept values (10,'ACCOUNTING','NEW YORK');
    insert into dept values (20,'RESEARCH','DALLAS');
    insert into dept values (30,'SALES','CHICAGO');
    insert into dept values (40,'OPERATIONS','BOSTON');

    insert into emp values (7369,'SMITH','CLERK',7902,to_date('17-12-1980','dd-mm-yyyy'),800,null,20);
    insert into emp values (7499,'ALLEN','SALESMAN',7698,to_date('20-2-1981','dd-mm-yyyy'),1600,300,30);
    insert into emp values (7521,'WARD','SALESMAN',7698,to_date('22-2-1981','dd-mm-yyyy'),1250,500,30);
    insert into emp values (7566,'JONES','MANAGER',7839,to_date('2-4-1981','dd-mm-yyyy'),2975,null,20);
    insert into emp values (7654,'MARTIN','SALESMAN',7698,to_date('28-9-1981','dd-mm-yyyy'),1250,1400,30);
    insert into emp values (7698,'BLAKE','MANAGER',7839,to_date('1-5-1981','dd-mm-yyyy'),2850,null,30);
    insert into emp values (7782,'CLARK','MANAGER',7839,to_date('9-6-1981','dd-mm-yyyy'),2450,null,10);
    insert into emp values (7788,'SCOTT','ANALYST',7566,to_date('13-JUL-87','dd-mm-rr')-85,3000,null,20);
    insert into emp values (7839,'KING','PRESIDENT',null,to_date('17-11-1981','dd-mm-yyyy'),5000,null,10);
    insert into emp values (7844,'TURNER','SALESMAN',7698,to_date('8-9-1981','dd-mm-yyyy'),1500,0,30);
    insert into emp values (7876,'ADAMS','CLERK',7788,to_date('13-JUL-87', 'dd-mm-rr')-51,1100,null,20);
    insert into emp values (7900,'JAMES','CLERK',7698,to_date('3-12-1981','dd-mm-yyyy'),950,null,30);
    insert into emp values (7902,'FORD','ANALYST',7566,to_date('3-12-1981','dd-mm-yyyy'),3000,null,20);
    insert into emp values (7934,'MILLER','CLERK',7782,to_date('23-1-1982','dd-mm-yyyy'),1300,null,10);
    commit;
    ```

Create a JSON-Duality view:

    ```sql
    create or replace json relational duality view department_dv as
    select json {
                '_id' : d.deptno,
                'departmentName'   : d.dname,
                'location'         : d.loc,
                'employees' :
                [ select json {'employeeNumber' : e.empno,
                                'employeeName'   : e.ename,
                                'job'            : e.job,
                                'salary'         : e.sal}
                    from   emp e with insert update delete
                    where  d.deptno = e.deptno ]}
    from dept d with insert update delete;
    ```

You can do using a script [json-dual](json-dual.sql), running in a shell:

    ```bash
    sqlplus vector/vector@"${VECTORDB}:1521/ORCLPDB1"  @"/Users/cdebari/Documents/GitHub/spring-ai-demo/json-dual.sql"
    ```

Let's check the content by connecting to the Oracle DB:

    ```bash
    sqlplus vector/vector@"${VECTORDB}:1521/ORCLPDB1"
    ```

And executing the following command, to pull the data using JSON duality:

    ```sql
    select json_serialize(d.data pretty) from department_dv d FETCH FIRST ROW ONLY;
    ```

Or directly from the `department_dv` table:

    ```sql
    select * from department_dv;
    ```

### Application

In the `application-dev.properties` files will be used the environment variables set at the step before:

    ```properties
    spring.ai.openai.api-key=${OPENAI_API_KEY}
    spring.ai.openai.base-url=${OPENAI_URL}
    spring.ai.openai.chat.options.model=${OPENAI_MODEL}
    spring.ai.openai.embedding.options.model=${OPENAI_EMBEDDING_MODEL}
    spring.ai.openai.chat.options.temperature=0.3
    spring.datasource.url=jdbc:oracle:thin:@${VECTORDB}:1521/ORCLPDB1
    spring.datasource.username=${DB_USER}
    spring.datasource.password=${DB_PASSWORD}
    spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
    config.tempDir=tempDir
    config.dropDb=true
    config.vectorDB=vectortable
    config.distance=EUCLIDEAN
    spring.servlet.multipart.max-file-size=10MB
    spring.servlet.multipart.max-request-size=20MB
    spring.ai.ollama.base-url=${OLLAMA_URL}
    spring.ai.ollama.embedding.options.model=${OLLAMA_EMBEDDINGS}
    spring.ai.ollama.chat.options.model=${OLLAMA_MODEL}
    ```

In the `application.properties` check if the default env is set as `dev`:

    ```properties
    spring.profiles.active=dev
    ```

Then build and run the application:

- Set env: `source ./env.sh`
- Build: `mvn clean package -Dmaven.test.skip=true`
- Run: `mvn spring-boot:run`

For each source update, repeat these two steps.

## 1. Test OpenAI version

Check code:

pom.xml:

    ```xml
        <!--//CHANGE-->
        <!-- Ollama for embeddings/chat
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
            </dependency>
        -->
    ```

DemoaiController.java:

    ```java
    //CHANGE
    //import org.springframework.ai.ollama.OllamaEmbeddingClient;
    //import org.springframework.ai.ollama.OllamaChatClient;
    ...

    //CHANGE
        private final EmbeddingClient embeddingClient;
        //private final OllamaEmbeddingClient embeddingClient;

        //CHANGE
        private final ChatClient chatClient;
        //private final OllamaChatClient chatClient;

    ...

    //CHANGE
        @Autowired
        public DemoaiController(EmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // OpenAI full
        //public DemoaiController(OllamaEmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // Ollama Embeddings - OpenAI Completion 
        //public DemoaiController(OllamaEmbeddingClient embeddingClient, OllamaChatClient chatClient, VectorService vectorService) { // Ollama full 

    ```

VectorService.java:

    ```java
    //CHANGE
    //import org.springframework.ai.ollama.OllamaChatClient;
    ...
    //CHANGE
        private final ChatClient aiClient;
        //private final OllamaChatClient aiClient;

        //CHANGE
        VectorService(@Qualifier("openAiChatClient") ChatClient aiClient) {
        //VectorService(OllamaChatClient aiClient) {
    ```

DemoaiApplication.java:

    ```java
    //CHANGE
    //import org.springframework.ai.ollama.OllamaEmbeddingClient;

    ...
    //CHANGE
        @Bean
        VectorStore vectorStore(EmbeddingClient ec, JdbcTemplate t) {
        //VectorStore vectorStore(OllamaEmbeddingClient ec, JdbcTemplate t) {
            return new OracleDBVectorStore(t, ec); 
        }

    ```

### Pre document store

#### Generic chat

    ```bash
    curl -X POST http://localhost:8080/ai/generate \
        -H "Content-Type: application/json" \
        -d '{"message":"What is a Generative AI?"}' | jq -r .generation
    ```

Here's a sample output from the command:

    ```text
    Generative AI refers to artificial intelligence systems that are capable of creating new content, such as images, text, or music, based on patterns and examples provided to them. These systems use algorithms and machine learning techniques to generate realistic and original content that mimics human creativity. Generative AI can be used in a variety of applications, such as creating art, writing stories, or designing products.
    ```

#### RAG request without any data stored in the DB

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
        -H "Content-Type: application/json" \
        -d '{"message":"Can I use any kind of development environment to run the example?"}'
    ```

Output from the command:

    ```json
    {
        "generation" : "Based on the provided documents, it is not specified whether any kind of development environment can be used to run the example. Therefore, I'm sorry but I haven't enough information to answer."
    }
    ```

### Search on data coming from a PDF stored

Store a PDF document in the DBMC 23c library: [**Oracle® Database: Get Started with Java Development**](https://docs.oracle.com/en/database/oracle/oracle-database/23/tdpjd/get-started-java-development.pdf) in the Oracle DB 23ai with embeddings coming from OpenAI Embedding service. Dowload locally, and run in a shell:

    ```bash
    curl -X POST -F "file=@./docs/get-started-java-development.pdf" http://localhost:8080/ai/store
    ```

>> **Note**: this process usually takes time because document will be splitted in hundreds or thousands of chunks, and for each one it will asked for an embeddings vector to OpenAI API service. In this case has been choosen a small document to wait a few seconds.

#### Q&A Sample

Let's look at some info in this document and try to query comparing the results with the actual content:

- **4.1.1 Oracle Database**

![dbtype](./img/dbtype.png)

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"Which kind of database you can use to run the Java Web example application) "}' | jq -r .generation
    ```

Response:

    ```text
    You can use either Oracle Autonomous Database or Oracle Database Free available on OTN to run the Java Web example application.
    ```

- **4.1.5 Integrated Development Environment**

![ide](./img/ide.png)

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"Can I use any kind of development environment to run the example?"}' | jq -r .generation
    ```

Response:

    ```text
    Based on the information provided in the documents, you can use an Integrated Development Environment (IDE) like IntelliJ Idea community version to develop the Java application that connects to the Oracle Database. The guide specifically mentions using IntelliJ Idea for creating and updating the files for the application. Therefore, it is recommended to use IntelliJ Idea as the development environment for running the example.
    ```

- **4.2 Verifying the Oracle Database Installation**

![dbverify](./img/dbverify.png)

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"To run the example, how can I check if the dbms it is working correctly?"}' | jq -r .generation
    ```

Response:

    ```text
    To check if the Oracle Database is working correctly, you can verify the installation by connecting to the database using the following commands:
    1. Navigate to the Oracle Database bin directory: $ cd $ORACLE_HOME/bin
    2. Connect to the database as sysdba: $ ./sqlplus / as sysdba

    If the connection is successful, you will see an output confirming that you are connected to the root container of the database. This indicates that the Oracle Database installation is working correctly. Additionally, you can download the Client Credentials for an ATP instance and verify the connection by following the steps provided in the documentation.
    ```

First, let's ask for a question not related to the document stored:

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
        -H "Content-Type: application/json" \
        -d '{"message":"How is the weather tomorrow?"}' | jq -r .generation
    ```

Response:

    ```json
    {
      "generation" : "I'm sorry but I haven't enough info to answer."
    }
    ```

Then, let's test similarity search for message **"To run the example, how can I check if the dbms it is working correctly?"** example. The `top_k` parameter determines how many nearest chunks to retrieve is set to **4** by default, and the result set is by default in reverse order. So, we need to execute the fololwing command:

    ```bash
    curl -X POST http://localhost:8080/ai/search-similar \
        -H "Content-Type: application/json" \
        -d '{"message":"To run the example, how can I check if the dbms it is working correctly?"}' | jq '.[3]'
    ```

Then, we test the deletion. Indexes begin counting at `1`, so let's execute the following command to delete occurrences 1, 4 and 5:

    ```bash
    curl "http://localhost:8080/ai/delete?id=1&id=5&id=4"
    ```

### JSON Duality semantic search example

1. First, we need to check whether `config.dropDb=true` is present in `application.properties`. This ensures that we drop all data from the database after every execution of our project, to ensure that we can test our endpoints without worrying about data retention -yet-. After we've set that, we restart the Maven project:

    ```bash
    mvn spring-boot:run
    ```

2. First, store the **DEPARTMENT_DV** JSON Duality view as bunch of embeddings:

    ```bash
    curl "http://localhost:8080/ai/store-json?view=DEPARTMENT_DV"
    ```

3. Let's ask simple queries:

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"Give me the list of names who work in the New York City department"}' | jq -r .generation
    ```

Response:

    ```json
    {
        "generation": "CLARK, KING, MILLER"
    }
    ```

Here's another example:

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"who has the largest salary among salesman and in which department he works? "}' | jq -r .generation
    ```

Response:

    ```text
    The salesman with the largest salary is ALLEN with a salary of 1600. He works in the SALES department located in CHICAGO.
    ```

Here's with a more complex question:

    ```bash
    curl -X POST http://localhost:8080/ai/rag \
    -H "Content-Type: application/json" \
    -d '{"message":"Give me the total of salaries of employees in the New York department"}' | jq -r .generation
    ```

This should return something similar to:

    ```text
    To calculate the total of salaries of employees in the New York department, we need to look at the first article which contains information about the ACCOUNTING department located in NEW YORK. The salaries of the employees in this department are as follows:
    - CLARK (MANAGER): $2450
    - KING (PRESIDENT): $5000
    - MILLER (CLERK): $1300

    Total of salaries in the New York department = $2450 + $5000 + $1300 = $8750

    Therefore, the total of salaries of employees in the New York department is $8750.
    ```

## 2. Runs with a private LLMs through OLLAMA

### OLLAMA setup

1. from OCI console, choose Compute/Instances menu:

    ![image](./img/instance.png)

2. Press **Create instance** button:

    ![image](./img/create.png)

3. Choose **VM.GPU.A10.2** shape, selecting **Virtual machine**/**Specialty and previous generation**:

    ![image](./img/shape.png)

4. Choose the Image **Oracle-Linux-8.9-Gen2-GPU-2024.02.26-0** from Oracle Linux 8 list of images:

    ![image](./img/image.png)

5. **Specify a custom boot volume size** and set 100 GB:

    ![image](./img/bootvolume.png)

6. Create the image.

7. At the end of creation process, obtain the **Public IPv4 address**, and with your private key (the one you generated or uploaded during creation), connect to:

    ```bash
    ssh -i ./<your_private>.key opc@[GPU_SERVER_IP]
    ```

8. Install and configure docker to use GPUs:

    ```bash
    sudo /usr/libexec/oci-growfs
    curl -s -L https://nvidia.github.io/libnvidia-container/stable/rpm/nvidia-container-toolkit.repo |   sudo tee /etc/yum.repos.d/nvidia-container-toolkit.repo
    sudo dnf install -y dnf-utils zip unzip
    sudo dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
    sudo dnf remove -y runc
    sudo dnf install -y docker-ce --nobest
    sudo useradd docker_user
    ```

9. We need to make sure that your Operating System user has permissions to run Docker containers. To do this, we can run the following command:

    ```bash
    sudo visudo
    ```

    And add this line at the end:

    ```bash
    docker_user  ALL=(ALL)  NOPASSWD: /usr/bin/docker
    ```

10. For convenience, we need to switch to our new user. For this, run:

    ```bash
    sudo su - docker_user
    ```

11. Finally, let's add an alias to execute Docker with admin privileges every time we type `docker` in our shell. For this, we need to modify a file, depending on your OS (in `.bash_profile` (MacOS) / `.bashrc` (Linux)). Insert, at the end of the file, this command:

    ```bash
    alias docker="sudo /usr/bin/docker"
    exit
    ```

12. Final installation

    ```bash
    sudo yum install -y nvidia-container-toolkit
    sudo nvidia-ctk runtime configure --runtime=docker
    sudo systemctl restart docker
    nvidia-ctk runtime configure --runtime=docker --config=$HOME/.config/docker/daemon.json
    ```

13. Re-connect to the VM, and run again:

    ```bash
    `sudo su - docker_user`
    ```

14. Run `docker` to check if everything it's ok.

15. Let's run a Docker container with the  `ollama/llama2` model for embeddings/completion:

    ```bash
    docker run -d --gpus=all -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama serve
    docker exec -it ollama ollama pull nomic-embed-text
    docker exec -it ollama ollama pull llama2:13b-chat-fp16
    docker logs -f --tail 10 ollama
    ```

Both the model, for embeddings/completion will run under the same server, and they will be addressed providing in the REST request for the specific model required.

To handle the firewall, we need to open port `11434` on our Security List. For this, let's:

1. In **Instance details** click on the **Virtual cloud network:** link:

    ![securitylist](./img/vcn.png)

2. In the menu **Resources** click on **Security Lists**:

    ![security](./img/securitylist.png)

3. Click on the link of **Default Security List...**

4. Click on the **Add Ingress Rules** button:

    ![security](./img/addIngress.png)

5. lick on the **Add Ingress Rules** button:

    ![security](./img/addIngress.png)

6. nsert details as shown in the following image and then click **Add Ingress Rules** button:

    ![security](./img/rule.png)

7. Update the `env.sh` file and run `source ./env.sh`:

    ```bash
    #export OPENAI_URL=http://[GPU_SERVER_IP]:3000
    export OPENAI_URL=https://api.openai.com
    #export OPENAI_MODEL=NousResearch--llama-2-7b-chat-hf
    export OPENAI_MODEL=gpt-3.5-turbo
    export OPENAI_EMBEDDING_MODEL=text-embedding-ada-002
    export VECTORDB=[VECTORDB_IP]
    export DB_USER=vector
    export DB_PASSWORD=vector
    export OLLAMA_URL=http://[GPU_SERVER_IP]:11434
    export OLLAMA_EMBEDDINGS=NousResearch--llama-2-7b-chat-hf
    export OLLAMA_MODEL=llama2:7b-chat-fp16
    export OPENAI_API_KEY=[YOUR_OPENAI_KEY]
    ```

8. Test with a shell running:

    ```xml
    curl ${OLLAMA_URL}/api/generate -d '{
        "model": "llama2:7b-chat-fp16",
        "prompt":"Why is the sky blue?"
    }'
    ```

### Customize for private LLMs: Vector Embeddings local, Open AI for Completion

* pom.xml: uncomment the ollama dependency:

```xml
    <!--//CHANGE-->	
	<!-- Ollama for embeddings -->
		<dependency>
			<groupId>org.springframework.ai</groupId>
			<artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
		 </dependency>
	 	<!--  -->
```

* DemoaiController.java - uncomment with final source code:

```java
    //CHANGE
    import org.springframework.ai.ollama.OllamaEmbeddingClient;
    //import org.springframework.ai.ollama.OllamaChatClient;
...

    //CHANGE
    //private final EmbeddingClient embeddingClient;
    private final OllamaEmbeddingClient embeddingClient;

    //CHANGE
    private final ChatClient chatClient;
    //private final OllamaChatClient chatClient;
...

    //CHANGE
    //public DemoaiController(EmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // OpenAI full
    public DemoaiController(OllamaEmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // Ollama Embeddings - OpenAI Completion 
    //public DemoaiController(OllamaEmbeddingClient embeddingClient, OllamaChatClient chatClient, VectorService vectorService) { // Ollama full 
        

```

VectorService.java - check if it's like this:

    ```java
    //CHANGE
    //import org.springframework.ai.ollama.OllamaChatClient;

    ...

    //CHANGE
        private final ChatClient aiClient;
        //private final OllamaChatClient aiClient;

        //CHANGE
        VectorService(@Qualifier("openAiChatClient") ChatClient aiClient) {
        //VectorService(OllamaChatClient aiClient) {

    ```

Test as done before. In the gpu docker logs, you'll see the chunks coming to be embedded.

### Full private LLMs with llama2:7b-chat-fp16

DemoaiController.java - uncomment with final source code:

    ```java
    //CHANGE
    import org.springframework.ai.ollama.OllamaEmbeddingClient;
    import org.springframework.ai.ollama.OllamaChatClient;

    ...
        //CHANGE
        //private final EmbeddingClient embeddingClient;
        private final OllamaEmbeddingClient embeddingClient;

        //CHANGE
        //private final ChatClient chatClient;
        private final OllamaChatClient chatClient;
    ...


    //CHANGE
        @Autowired
        //public DemoaiController(EmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // OpenAI full
        //public DemoaiController(OllamaEmbeddingClient embeddingClient, @Qualifier("openAiChatClient") ChatClient chatClient, VectorService vectorService) {  // Ollama Embeddings - OpenAI Completion 
        public DemoaiController(OllamaEmbeddingClient embeddingClient, OllamaChatClient chatClient, VectorService vectorService) { // Ollama full 
        

    ```

VectorService.java - uncomment with final source code:

    ```java
    //CHANGE
    import org.springframework.ai.ollama.OllamaChatClient;

    ...


        //CHANGE
        //private final ChatClient aiClient;
        private final OllamaChatClient aiClient;

        //CHANGE
        //VectorService(@Qualifier("openAiChatClient") ChatClient aiClient) {
        VectorService(OllamaChatClient aiClient) {
    ```

Test as before. Now, you could see a tiny degradation of quality (since the model is quite small), mantaining the same embeddings and context retrieval. For example, with the question:

    ```text
    Q: Which kind of database you can use to run the Java Web example application:
    A: The document states that the application uses Oracle JDBC Thin driver, Universal Connection Pool (UCP), and Java in the Database (using embedded OJVM). Therefore, you can use an Oracle database to run the Java Web example application.
    ```text

This first result doesn't return a very good, personalized result. However, if we  good. But for:

    ```text
    Q: Can I use any kind of development environment to run the example?
    A: the user can use any development environment to run the example as long as it has a Java compiler and a database manager installed. The DOCUMENTS text does not specify any particular IDE that must be used, so any IDE with Java development capabilities should work. However, the guide does recommend using IntelliJ Idea Community Edition for ease of development, but this is not a requirement.\n\nTo answer the user's question, you could say: \"Yes, you can use any development environment to run the example as long as it has a Java compiler and a database manager installed. While the guide recommends using IntelliJ Idea Community Edition for ease of development, any IDE with Java development capabilities should work.\
    ```

The result is more customized and acceptable.

This trade-off in using private LLMs model could be overcome choosing *larger models*, enough to mantain a good quality.

> **Note**: the number of billions of parameters of a model version usually has a direct correlation with the size of the model, and its generation quality. The higher, the better, although you also need to watch out for OOM (out of memory) errors and a slower generation throughput.

## 3. Deploy on Oracle Backend for Spring Boot and Microservices

Let's show what Oraclecan offer to deploy on an enterprise grade the GenAI application developed so far.

The platform [**Oracle Backend for Spring Boot and Microservices**](https://oracle.github.io/microservices-datadriven/spring/) allows developers to build microservices in Spring Boot and provision a backend as a service with the Oracle Database and other infrastructure components that operate on multiple clouds. This service vastly simplifies the task of building, testing, and operating microservices platforms for reliable, secure, and scalable enterprise applications.

To setup this platform, follow the instruction included in **Lab1: Provision an instance** and **Lab 2: Setup your Development Environment** of the [LiveLabs: CloudBank - Building an App with Spring Boot and Mobile APIs with Oracle Database and Kubernetes](https://apexapps.oracle.com/pls/apex/f?p=133:180:7384418726808::::wid:3607). At the end, proceed with the following steps:

1. In the `application.properties` change the active env as `prod`:

    ```properties
    spring.profiles.active=prod
    ```

2. In the `application-prod.properties`, change the parameters in `< >` with the values set in `env.sh`:

    ```properties
    spring.ai.openai.api-key=<OPENAI_API_KEY>
    spring.ai.openai.base-url=<OPENAI_URL>
    spring.ai.openai.chat.options.model=gpt-3.5-turbo
    spring.ai.openai.embedding.options.model=text-embedding-ada-002
    spring.datasource.url=jdbc:oracle:thin:@<VECTORDB>:1521/ORCLPDB1
    spring.datasource.username=vector
    spring.datasource.password=vector
    spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
    config.tempDir=tempDir
    config.dropDb=true
    config.vectorDB=vectortable
    config.distance=EUCLIDEAN
    spring.servlet.multipart.max-file-size=10MB
    spring.servlet.multipart.max-request-size=20MB
    spring.ai.ollama.base-url=<OLLAMA_URL>
    spring.ai.ollama.embedding.options.model=nomic-embed-text
    spring.ai.ollama.chat.options.model=llama2:7b-chat-fp16
    ```

3. Open a terminal, and using the **Kubernetes** admin command, open a port forward to the backend:

    ```bash
    kubectl -n obaas-admin port-forward svc/obaas-admin 8080:8080
    ```

4. Using the command-line tool `oractl`, deploy the application running the following commands:

    ```bash
    oractl:>connect
    ? username obaas-admin
    ? password **************

    oractl:>create --app-name rag
    oractl:>deploy --app-name rag --service-name demoai --artifact-path /Users/cdebari/Documents/GitHub/spring-ai-demo/target/demoai-0.0.1-SNAPSHOT.jar --image-version 0.0.1 --service-profile prod

    ```

5. Let's test the application with port forwarding. First, we need to stop the current `demoai` instance running on the background, to free the previous port being used; and, in a different terminal, run a port forwarding on port 8080 to the remote service on the **Oracle Backend for Spring Boot and Microservices**:

    ```bash
    kubectl -n rag port-forward svc/demoai 8080:8080
    ```

6. In a different terminal, test the service as done before, for example:

    ```bash
        curl -X POST http://localhost:8080/ai/rag \
        -H "Content-Type: application/json" \
        -d '{"message":"How is computed the Interest? Give me as much details as you can"}' | jq -r .generation
    ```

## Notes/Issues

Additional Use Cases like summarization and embedding coming soon.

## URLs

- [Oracle AI](https://www.oracle.com/artificial-intelligence/)
- [AI for Developers](https://developer.oracle.com/technologies/ai.html)

## Contributing

This project is open source.  Please submit your contributions by forking this repository and submitting a pull request!  Oracle appreciates any contributions that are made by the open-source community.

## License

Copyright (c) 2024 Oracle and/or its affiliates.

Licensed under the Universal Permissive License (UPL), Version 1.0.

See [LICENSE](LICENSE) for more details.

ORACLE AND ITS AFFILIATES DO NOT PROVIDE ANY WARRANTY WHATSOEVER, EXPRESS OR IMPLIED, FOR ANY SOFTWARE, MATERIAL OR CONTENT OF ANY KIND CONTAINED OR PRODUCED WITHIN THIS REPOSITORY, AND IN PARTICULAR SPECIFICALLY DISCLAIM ANY AND ALL IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY, AND FITNESS FOR A PARTICULAR PURPOSE.  FURTHERMORE, ORACLE AND ITS AFFILIATES DO NOT REPRESENT THAT ANY CUSTOMARY SECURITY REVIEW HAS BEEN PERFORMED WITH RESPECT TO ANY SOFTWARE, MATERIAL OR CONTENT CONTAINED OR PRODUCED WITHIN THIS REPOSITORY. IN ADDITION, AND WITHOUT LIMITING THE FOREGOING, THIRD PARTIES MAY HAVE POSTED SOFTWARE, MATERIAL OR CONTENT TO THIS REPOSITORY WITHOUT ANY REVIEW. USE AT YOUR OWN RISK.
