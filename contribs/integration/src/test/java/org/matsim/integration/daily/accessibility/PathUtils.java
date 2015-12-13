/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.integration.daily.accessibility;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author nagel
 *
 */
class PathUtils {
	private static final Logger log = Logger.getLogger( PathUtils.class ) ;
	
	private PathUtils(){} // do not instantiate

	static String tryANumberOfFolderStructures(String folderStructure, String networkFile) {
		String currentDir = new File("tmp").getAbsolutePath();
		currentDir = currentDir.substring(0, currentDir.length() - 3);
		log.info("base directory: " + currentDir);
		
		try {
			final String filename = folderStructure + networkFile;
			log.info( "trying " + filename );
			IOUtils.getBufferedReader( filename ) ;
		} catch ( Exception ee ) {
			String prevFolderStructure = folderStructure ;
			folderStructure = "../../" ; // build server
			log.info( "did not find file with folderStructure=" + prevFolderStructure + ", next trying with " + folderStructure ) ;
		}
		try { 
			final String filename = folderStructure + networkFile;
			log.info( "trying " + filename );
			IOUtils.getBufferedReader( filename ) ;
		} catch ( Exception ee ) {
			String prevFolderStructure = folderStructure ;
			folderStructure = "../../../" ; // local on dz's computer
			log.info( "did not find file with folderStructure=" + prevFolderStructure + ", next trying with " + folderStructure ) ;
		}
		try { 
			final String filename = folderStructure + networkFile;
			log.info( "trying " + filename );
			IOUtils.getBufferedReader( filename ) ;
		} catch ( Exception ee ) {
			log.info( "did not find file with folderStructure=" + folderStructure + ", giving up") ;
//			throw new RuntimeException( "cannot find file" ) ;
		}
		return folderStructure ;
	}

}
