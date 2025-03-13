package org.reactome.server.tools.diagram.exporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactome.server.graph.config.GraphCoreNeo4jConfig;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ContextConfiguration(classes = {GraphCoreNeo4jConfig.class})
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {

    @Value("${spring.neo4j.uri}")
    private String neo4jURI;

    @Value("${spring.neo4j.authentication.username}")
    private String neo4jUser;

    @Value("${spring.neo4j.authentication.password}")
    private String neo4jPassword;

    @Value("${spring.data.neo4j.database}")
    private String database;

    @BeforeAll
    public void initGraph() {
        ReactomeGraphCore.initialise(this.neo4jURI, this.neo4jUser, this.neo4jPassword, this.database);
    }
}
