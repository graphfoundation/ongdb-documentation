/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.server;

import java.net.URI;

import org.dummy.doc.web.service.DummyThirdPartyWebService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.neo4j.doc.server.helpers.CommunityServerBuilder;
import org.neo4j.doc.server.helpers.FunctionalTestHelper;
import org.neo4j.doc.server.helpers.ServerHelper;
import org.neo4j.doc.server.helpers.Transactor;
import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RestRequest;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.server.NeoServer;

import static org.junit.Assert.assertEquals;

import static org.neo4j.doc.server.helpers.FunctionalTestHelper.CLIENT;

public class NeoServerJAXRSDocIT extends ExclusiveServerTestBase
{
    private NeoServer server;

    @Before
    public void cleanTheDatabase()
    {
        ServerHelper.cleanTheDatabase( server );
    }

    @After
    public void stopServer()
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Test
    public void shouldMakeJAXRSClassesAvailableViaHTTP() throws Exception
    {
        CommunityServerBuilder builder = CommunityServerBuilder.server();
        server = ServerHelper.createNonPersistentServer( builder );
        FunctionalTestHelper functionalTestHelper = new FunctionalTestHelper( server );

        JaxRsResponse response = new RestRequest().get( functionalTestHelper.managementUri() );
        assertEquals( 200, response.getStatus() );
    }

    @Test
    public void shouldLoadThirdPartyJaxRsClasses() throws Exception
    {
        server = CommunityServerBuilder.server()
                .withThirdPartyJaxRsPackage( "org.dummy.doc.web.service",
                        DummyThirdPartyWebService.DUMMY_WEB_SERVICE_MOUNT_POINT )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();

        URI thirdPartyServiceUri = new URI( server.baseUri()
                .toString() + DummyThirdPartyWebService.DUMMY_WEB_SERVICE_MOUNT_POINT ).normalize();
        String response = CLIENT.resource( thirdPartyServiceUri.toString() )
                .get( String.class );
        assertEquals( "hello", response );

        // Assert that extensions gets initialized
        int nodesCreated = createSimpleDatabase( server.getDatabase().getGraph() );
        thirdPartyServiceUri = new URI( server.baseUri()
                .toString() + DummyThirdPartyWebService.DUMMY_WEB_SERVICE_MOUNT_POINT + "/inject-test" ).normalize();
        response = CLIENT.resource( thirdPartyServiceUri.toString() )
                .get( String.class );
        assertEquals( String.valueOf( nodesCreated ), response );
    }

    private int createSimpleDatabase( final GraphDatabaseAPI graph )
    {
        final int numberOfNodes = 10;
        new Transactor( graph, () -> {
            for ( int i = 0; i < numberOfNodes; i++ )
            {
                graph.createNode();
            }

            for ( Node n1 : graph.getAllNodes() )
            {
                for ( Node n2 : graph.getAllNodes() )
                {
                    if ( n1.equals( n2 ) )
                    {
                        continue;
                    }

                    n1.createRelationshipTo( n2, RelationshipType.withName( "REL" ) );
                }
            }
        }).execute();

        return numberOfNodes;
    }
}
