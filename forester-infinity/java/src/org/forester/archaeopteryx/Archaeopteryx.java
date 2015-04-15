// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2008-2009 Christian M. Zmasek
// Copyright (C) 2008-2009 Burnham Institute for Medical Research
// All rights reserved
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
//
// Contact: phylosoft @ gmail . com
// WWW: www.phylosoft.org/forester

package org.forester.archaeopteryx;

import java.io.File;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.nexus.NexusPhylogeniesParser;
import org.forester.io.parsers.nhx.NHXParser;
import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.util.ForesterUtil;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.*;
import java.net.URL;
import java.util.UUID;
import java.util.Date;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Scanner;

//
// java -javaagent:shiftone-jrat.jar -cp
// $HOME/SOFTWARE_DEV/ECLIPSE_WORKSPACE/forester-atv/java/forester.jar:.
// org.forester.archaeopteryx.Archaeopteryx
// -c $HOME/SOFTWARE_DEV/ECLIPSE_WORKSPACE/forester-atv/_aptx_configuration_file
//
public final class Archaeopteryx {

    public static MainFrame createApplication( final Phylogeny phylogeny ) {
        final Phylogeny[] phylogenies = new Phylogeny[ 1 ];
        phylogenies[ 0 ] = phylogeny;
        return createApplication( phylogenies, "", "" );
    }

    public static MainFrame createApplication( final Phylogeny[] phylogenies ) {
        return createApplication( phylogenies, "", "" );
    }

    public static MainFrame createApplication( final Phylogeny[] phylogenies,
                                               final String config_file_name,
                                               final String title ) {
        return MainFrameApplication.createInstance( phylogenies, config_file_name, title );
    }

    public static String getUniqueFileName(String directory, String extension) {
	    Date date = new Date();
	    return new File(directory, new StringBuilder().append("prefix")
		.append(date.getTime()).append(UUID.randomUUID())
		.append(".").append(extension).toString()).toString();
    }

    public static void main( final String args[] ) {
        Phylogeny[] phylogenies = null;
        String config_filename = null;
        String monitor_url = null;
        Configuration conf = null;
        File f = null;
        int filename_index = 0;
        try {
            if ( args.length == 0 ) {
                conf = new Configuration( null, false, false, true );
            }
            else if ( args.length > 0 ) {
                // check for a config file
                if ( args[ 0 ].startsWith( "-c" ) ) {
                    config_filename = args[ 1 ];
                    filename_index += 2;
                }
                if ( args[ 0 ].startsWith( "-open" ) ) {
                    filename_index += 1;
                }
                conf = new Configuration( config_filename, false, false, true );
                if ( args.length > filename_index ) {
                    f = new File( args[ filename_index ] );
                    String err = ForesterUtil.isReadableFile( f );
                    if ( !ForesterUtil.isEmpty( err ) ) {
			// try stripping the path and checking the current working directory
			String local_file = args[ filename_index ];
			int slash = local_file.lastIndexOf("/");
			if(slash >= 0){
				local_file = local_file.substring(slash+1);
	                        f = new File( local_file );
	                        err = ForesterUtil.isReadableFile( f );
			}
		    }
                    if ( !ForesterUtil.isEmpty( err ) ) {
			if( args[ filename_index ].startsWith("http:") ){
				System.err.println("attempting to open URL for " + args[ filename_index ] );
				monitor_url = args[ filename_index ];
				URL website = new URL(args[ filename_index ]);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
//				String tmpfilename = getUniqueFileName("./", ".xml");
				String tmpfilename = getUniqueFileName("./", ".nwk");
				FileOutputStream fos = new FileOutputStream(tmpfilename);
				fos.getChannel().transferFrom(rbc, 0, 1 << 24);
				args[ filename_index ] = tmpfilename;
				f = new File( tmpfilename );
			}else{
	                        ForesterUtil.fatalError( Constants.PRG_NAME, err );
			}
                    }
                    boolean nhx_or_nexus = false;
                    final PhylogenyParser p = ParserUtils.createParserDependingOnFileType( f, conf
                            .isValidatePhyloXmlAgainstSchema() );
                    if ( p instanceof NHXParser ) {
                        nhx_or_nexus = true;
                        final NHXParser nhx = ( NHXParser ) p;
                        nhx.setReplaceUnderscores( conf.isReplaceUnderscoresInNhParsing() );
                        nhx.setIgnoreQuotes( false );
                        PhylogenyMethods.TAXONOMY_EXTRACTION te = PhylogenyMethods.TAXONOMY_EXTRACTION.NO;
                        if ( conf.isExtractPfamTaxonomyCodesInNhParsing() ) {
                            te = PhylogenyMethods.TAXONOMY_EXTRACTION.PFAM_STYLE_ONLY;
                        }
                        nhx.setTaxonomyExtraction( te );
                    }
                    else if ( p instanceof NexusPhylogeniesParser ) {
                        nhx_or_nexus = true;
                        final NexusPhylogeniesParser nex = ( NexusPhylogeniesParser ) p;
                        nex.setReplaceUnderscores( conf.isReplaceUnderscoresInNhParsing() );
                        nex.setIgnoreQuotes( false );
                    }
                    else if ( p instanceof PhyloXmlParser ) {
                        MainFrameApplication.warnIfNotPhyloXmlValidation( conf );
                    }
                    phylogenies = PhylogenyMethods.readPhylogenies( p, f );
                    if ( nhx_or_nexus && conf.isInternalNumberAreConfidenceForNhParsing() ) {
                        for( final Phylogeny phy : phylogenies ) {
                            PhylogenyMethods.transferInternalNodeNamesToConfidence( phy );
                        }
                    }
                }
            }
        }
        catch ( final Exception e ) {
            ForesterUtil.fatalError( Constants.PRG_NAME, "failed to start: " + e.getLocalizedMessage() );
        }
        String title = "";
        if ( f != null ) {
            title = f.getName();
        }
        File current_dir = null;
        if ( ( phylogenies != null ) && ( phylogenies.length > 0 ) ) {
            current_dir = new File( "." );
        }
        try {
        	MainFrame mfa = MainFrameApplication.createInstance( phylogenies, conf, title, current_dir );

        	while(true){
        		Thread.sleep(4000);
//    	    	System.err.println("re-opening URL for " + monitor_url );
    			URL website = new URL(monitor_url);
    			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
    			String old_content = new Scanner(new File(args[ filename_index ])).useDelimiter("\\Z").next();
    			String tmpfilename = args[ filename_index ];
    			FileOutputStream fos = new FileOutputStream(tmpfilename);
    			fos.getChannel().transferFrom(rbc, 0, 1 << 24);
    			f = new File( tmpfilename );
    			String new_content = new Scanner(new File(args[ filename_index ])).useDelimiter("\\Z").next();
    			
    			if(old_content.equals(new_content)){
 //   				System.err.println("no change\n");
    				continue;
    			}else{
/*    				System.err.println("Old was:\n" + old_content);
    				System.err.println("New is:\n" + new_content);
*/    			}

    			// parse the new phylogeny
                boolean nhx_or_nexus = false;
                final PhylogenyParser p = ParserUtils.createParserDependingOnFileType( f, conf
                        .isValidatePhyloXmlAgainstSchema() );
                if ( p instanceof NexusPhylogeniesParser ) {
                    nhx_or_nexus = true;
                    final NexusPhylogeniesParser nex = ( NexusPhylogeniesParser ) p;
                    nex.setReplaceUnderscores( conf.isReplaceUnderscoresInNhParsing() );
                    nex.setIgnoreQuotes( false );
                }
                phylogenies = PhylogenyMethods.readPhylogenies( p, f );
                if ( nhx_or_nexus && conf.isInternalNumberAreConfidenceForNhParsing() ) {
                    for( final Phylogeny phy : phylogenies ) {
                        PhylogenyMethods.transferInternalNodeNamesToConfidence( phy );
                    }
                }
                ((MainFrameApplication)mfa).addPhylogenies(phylogenies);
                
        	}
        
        }
        catch ( final Exception ex ) {
            AptxUtil.unexpectedException( ex );
        }
        catch ( final Error err ) {
            AptxUtil.unexpectedError( err );
        }
    }
}