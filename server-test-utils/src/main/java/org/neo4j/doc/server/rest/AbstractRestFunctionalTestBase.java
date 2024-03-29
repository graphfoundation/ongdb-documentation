/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.doc.server.rest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Rule;

import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.SharedServerTestBase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;

import static org.junit.Assert.assertEquals;

import static org.neo4j.server.rest.domain.JsonHelper.createJsonFrom;
import static org.neo4j.server.rest.web.Surface.PATH_NODES;
import static org.neo4j.server.rest.web.Surface.PATH_NODE_INDEX;
import static org.neo4j.server.rest.web.Surface.PATH_RELATIONSHIPS;
import static org.neo4j.server.rest.web.Surface.PATH_RELATIONSHIP_INDEX;
import static org.neo4j.server.rest.web.Surface.PATH_SCHEMA_CONSTRAINT;
import static org.neo4j.server.rest.web.Surface.PATH_SCHEMA_INDEX;

public class AbstractRestFunctionalTestBase extends SharedServerTestBase implements GraphHolder {
    protected static final String NODES = "http://localhost:7474/db/data/node/";

    @Rule
    public TestData<Map<String, Node>> data = TestData.producedThrough(GraphDescription.createGraphFor(this, true));

    @Rule
    public TestData<RESTDocsGenerator> gen = TestData.producedThrough(RESTDocsGenerator.PRODUCER);

    @Before
    public void setUp() {
        gen().setSection(getDocumentationSectionName());
        gen().setGraph(graphdb());
    }

    @SafeVarargs
    public final String doCypherRestCall(String endpoint, String scriptTemplate, Status status,
                                         Pair<String, String>... params) {
        String parameterString = createParameterString(params);

        return doCypherRestCall(endpoint, scriptTemplate, status, parameterString);
    }

    public String doCypherRestCall(String endpoint, String scriptTemplate, Status status, String parameterString) {
        data.get();

        String script = createScript(scriptTemplate);
        String queryString = "{\"query\": \"" + script + "\",\"params\":{" + parameterString + "}}";

        String snippet = org.neo4j.cypher.internal.compiler.v3_4.prettifier.Prettifier$.MODULE$.apply(script);
        gen().expectedStatus(status.getStatusCode())
                .payload(queryString)
                .description(AsciidocHelper.createAsciiDocSnippet("cypher", snippet));
        return gen().post(endpoint).entity();
    }

    private Long idFor(String name) {
        return data.get().get(name).getId();
    }

    private String createParameterString(Pair<String, String>[] params) {
        String paramString = "";
        for (Pair<String, String> param : params) {
            String delimiter = paramString.isEmpty() || paramString.endsWith("{") ? "" : ",";

            paramString += delimiter + "\"" + param.first() + "\":\"" + param.other() + "\"";
        }

        return paramString;
    }

    protected String createScript(String template) {
        for (String key : data.get().keySet()) {
            template = template.replace("%" + key + "%", idFor(key).toString());
        }
        return template;
    }

    protected String startGraph(String name) {
        return AsciidocHelper.createGraphVizWithNodeId("Starting Graph", graphdb(), name);
    }

    @Override
    public GraphDatabaseService graphdb() {
        return SharedServerTestBase.server().getDatabase().getGraph();
    }

    protected static String getDataUri() {
        return "http://localhost:7474/db/data/";
    }

    protected String getDatabaseUri() {
        return "http://localhost:7474/db/";
    }

    protected String getNodeUri(Node node) {
        return getNodeUri(node.getId());
    }

    protected String txUri() {
        return getDataUri() + "transaction";
    }

    protected static String txCommitUri() {
        return getDataUri() + "transaction/commit";
    }

    protected String txUri(long txId) {
        return getDataUri() + "transaction/" + txId;
    }

    public static long extractTxId(HTTP.Response response) {
        int lastSlash = response.location().lastIndexOf("/");
        String txIdString = response.location().substring(lastSlash + 1);
        return Long.parseLong(txIdString);
    }

    protected String getNodeUri(long node) {
        return getDataUri() + PATH_NODES + "/" + node;
    }

    protected String getRelationshipUri(Relationship relationship) {
        return getDataUri() + PATH_RELATIONSHIPS + "/" + relationship.getId();
    }

    protected String postNodeIndexUri(String indexName) {
        return getDataUri() + PATH_NODE_INDEX + "/" + indexName;
    }

    protected String postRelationshipIndexUri(String indexName) {
        return getDataUri() + PATH_RELATIONSHIP_INDEX + "/" + indexName;
    }

    protected Node getNode(String name) {
        return data.get().get(name);
    }

    protected Node[] getNodes(String... names) {
        Node[] nodes = {};
        ArrayList<Node> result = new ArrayList<>();
        for (String name : names) {
            result.add(getNode(name));
        }
        return result.toArray(nodes);
    }

    public void assertSize(int expectedSize, String entity) {
        Collection<?> hits;
        try {
            hits = (Collection<?>) JsonHelper.readJson(entity);
            assertEquals(expectedSize, hits.size());
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPropertiesUri(Relationship rel) {
        return getRelationshipUri(rel) + "/properties";
    }

    public String getPropertiesUri(Node node) {
        return getNodeUri(node) + "/properties";
    }

    public RESTDocsGenerator gen() {
        return gen.get();
    }

    protected String getDocumentationSectionName() {
        return "dev/rest-api";
    }

    public String getLabelsUri() {
        return format("%slabels", getDataUri());
    }

    public String getPropertyKeysUri() {
        return format("%spropertykeys", getDataUri());
    }

    public String getNodesWithLabelUri(String label) {
        return format("%slabel/%s/nodes", getDataUri(), label);
    }

    public String getNodesWithLabelAndPropertyUri(String label, String property, Object value) throws UnsupportedEncodingException {
        return format("%slabel/%s/nodes?%s=%s", getDataUri(), label, property,
                encode(JsonHelper.createJsonFrom(value), StandardCharsets.UTF_8.name()));
    }

    public String getSchemaIndexUri() {
        return getDataUri() + PATH_SCHEMA_INDEX;
    }

    public String getSchemaIndexLabelUri(String label) {
        return getDataUri() + PATH_SCHEMA_INDEX + "/" + label;
    }

    public String getSchemaIndexLabelPropertyUri(String label, String property) {
        return getDataUri() + PATH_SCHEMA_INDEX + "/" + label + "/" + property;
    }

    public String getSchemaConstraintUri() {
        return getDataUri() + PATH_SCHEMA_CONSTRAINT;
    }

    public String getSchemaConstraintLabelUri(String label) {
        return getDataUri() + PATH_SCHEMA_CONSTRAINT + "/" + label;
    }

    public String getSchemaConstraintLabelUniquenessUri(String label) {
        return getDataUri() + PATH_SCHEMA_CONSTRAINT + "/" + label + "/uniqueness/";
    }

    public String getSchemaConstraintLabelUniquenessPropertyUri(String label, String property) {
        return getDataUri() + PATH_SCHEMA_CONSTRAINT + "/" + label + "/uniqueness/" + property;
    }

}
