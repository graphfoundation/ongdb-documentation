/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Stack;

import org.neo4j.doc.tools.AsciiDocGenerator;
import org.neo4j.helpers.Exceptions;
import org.neo4j.shell.impl.CollectingOutput;
import org.neo4j.shell.impl.RemoteOutput;
import org.neo4j.shell.impl.SameJvmClient;

import static org.junit.Assert.*;

public class Documenter
{
    public static class DocOutput implements Output, Serializable
    {
        private static final long serialVersionUID = 1L;
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private PrintWriter out = new PrintWriter( baos );

        @Override
        public Appendable append( final CharSequence csq, final int start, final int end )
        throws IOException
        {
            this.print( RemoteOutput.asString( csq ).substring( start, end ) );
            return this;
        }

        @Override
        public Appendable append( final char c ) throws IOException
        {
            this.print( c );
            return this;
        }

        @Override
        public Appendable append( final CharSequence csq ) throws IOException
        {
            this.print( RemoteOutput.asString( csq ) );
            return this;
        }

        @Override
        public void println( final Serializable object ) throws RemoteException
        {
            out.println( object );
            out.flush();
        }

        @Override
        public void println() throws RemoteException
        {
            out.println();
            out.flush();
        }

        @Override
        public void print( final Serializable object ) throws RemoteException
        {
            out.print( object );
            out.flush();
        }
    }

    public class Job
    {
        public final String query;
        public final String assertion;
        public final String comment;

        public Job( final String query, final String assertion, final String comment )
        {
            this.query = query;
            this.assertion = assertion;
            this.comment = comment;
        }
    }

    private final String title;
    private final Stack<Job> stack = new Stack<>();
    private final ShellClient client;

    public Documenter( final String title, final ShellServer server )
    {
        this.title = title;
        try
        {
            this.client = new SameJvmClient(new HashMap<>(), server,
                    new CollectingOutput(), InterruptSignalHandler.getHandler() );
        }
        catch ( Exception e )
        {
            throw Exceptions.launderedException( "Error creating client",e );
        }
    }

    public void add( final String query, final String assertion, final String comment )
    {
        stack.push( new Job( query, assertion, comment ) );
    }

    public void run()
    {
        PrintWriter out = getWriter( this.title );
        out.println();
        out.println( "[source, bash]" );
        out.println( "-----" );

        for ( Job job : stack )
        {
            try
            {
                DocOutput output = new DocOutput();
                String prompt = client.getPrompt();
                client.evaluate( job.query, output );
                String result = output.baos.toString();
                assertTrue( result + "did not contain " + job.assertion, result.contains( job.assertion ) );
                doc( job, out, result, prompt );
            }
            catch ( ShellException e )
            {
                throw new RuntimeException( e );
            }
        }
        out.println( "-----" );
        out.flush();
        out.close();
    }

    PrintWriter getWriter( String title )
    {
        return AsciiDocGenerator.getPrintWriter( "target/docs/dev/shell", title );
    }

    private void doc( final Job job, final PrintWriter out, final String result, String prompt )
    {
        if ( job.query != null && !job.query.isEmpty() )
        {
            out.println( " # " + job.comment );
            out.println( " " + prompt + job.query );
            if ( result != null && !result.isEmpty() )
            {
                out.println( " " + result.replace( "\n", "\n " ) );
            }
            out.println();
        }
    }
}
