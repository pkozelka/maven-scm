/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.scm.provider.hg.command.changelog;

import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.log.DefaultLog;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Petr Kozelka
 */
public class HgChangeLogConsumerTest
{

    /**
     * This method is quite generic; it allows direct execution both from IDEs and commandline, and both from
     * "this project basedir" or any parent's project basedir.
     *
     * @param resourceName classpath resource name, starting with slash
     * @return file corresponding to that resource
     * @throws FileNotFoundException -
     */
    private File getFile(String resourceName)
        throws FileNotFoundException
    {
        final URL resource = getClass().getResource( resourceName );
        if ( resource == null ) {
            throw new FileNotFoundException( resourceName );
        }
        return new File( resource.getPath() );
    }

    private static void consumeFile( File file, StreamConsumer consumer )
        throws IOException
    {
        final BufferedReader r = new BufferedReader( new FileReader( file ) );
        try
        {
            String line = r.readLine();
            while ( line != null ) {
                consumer.consumeLine( line );
                line = r.readLine();
            }
        }
        finally
        {
            r.close();
        }
    }

    @Test
    public void testConsumer()
        throws Exception
    {
        final HgChangeLogConsumer consumer = new HgChangeLogConsumer( new DefaultLog(), null);
        // following log was generated with "hg --verbose log" from project http://code.google.com/p/gwt-customuibinder/
        consumeFile( getFile( "/hg/changelog/gwt-customuibinder.hglog.txt" ), consumer);

        final List<ChangeSet> changeLog = consumer.getModifications();
        final Map<String, AtomicInteger> fileCountsPerExtension = new LinkedHashMap<String, AtomicInteger>(  );
        for ( ChangeSet changeSet : changeLog )
        {
            for ( ChangeFile changeFile : changeSet.getFiles() )
            {
                final int n = changeFile.getName().lastIndexOf( '.' );
                if ( n > 0 ) { // ignore files starting with dot
                    final String ext = changeFile.getName().substring( n );
                    if ( ! fileCountsPerExtension.containsKey( ext ) ) {
                        fileCountsPerExtension.put( ext, new AtomicInteger() );
                    }
                    fileCountsPerExtension.get( ext ).incrementAndGet();
                }
            }
        }
        // note: if you use different logfile, it's recommended to count following stats using other tools than error message from failing junit
        Assert.assertEquals( "{.xml=22, .java=23, .prefs=3, .txt=1}", fileCountsPerExtension.toString() );
    }
}
