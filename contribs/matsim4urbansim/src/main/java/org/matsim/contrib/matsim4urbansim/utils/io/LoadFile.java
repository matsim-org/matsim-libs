/* *********************************************************************** *
 * project: org.matsim.*
 * LoadMATSim4UrbanSimXSD.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.utils.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;



/**
 * @author thomas
 *
 */
@Deprecated // should be replaced by matsim core xml parsing. kai, jul'13
public class LoadFile {
	
	// logger
	private static final Logger log = Logger.getLogger(LoadFile.class);
	
	private String source 			= null;
	private String destinationPath	= null;
	private String fileName 		= null;
		
	/**
	 * constructor
	 * @param source
	 * @param destination
	 */
	@Deprecated // should be replaced by matsim core xml parsing. kai, jul'13
	public LoadFile(String source, String destination, String fileName){
		this.source = source;
		this.destinationPath = Paths.checkPathEnding( destination );
		this.fileName = fileName;
	}
	
	/**
	 * linke loadMATSim4UrbanSimXSD() but returns the canonical path
	 * of the loaded xsd file
	 * @return path of loaded xsd file
	 */
	@Deprecated // should be replaced by matsim core xml parsing. kai, jul'13
	public String loadMATSim4UrbanSimXSDString(){
		
		try {
			File f = loadMATSim4UrbanSimXSD();
			return f.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * creates a local copy of the MATSim4UrbanSim xsd file from matsim.org
	 * and returns the reference to the created file
	 * @return reference to the created xsd file
	 */
	@Deprecated // should be replaced by matsim core xml parsing. kai, jul'13
	public File loadMATSim4UrbanSimXSD(){
		
		InputStream is = null;
		BufferedOutputStream bos = null;
		boolean isInternetStream = Boolean.TRUE;
		
		// make sure output directory exists
		File dir = new File(this.destinationPath);
		if(!dir.exists())
			dir.mkdirs();
		File output = new File(destinationPath + fileName);
		
		log.info("Trying to load " + this.source + ". In some cases (e.g. network interface up but no connection), this may take a bit.");
		log.info("The xsd file will be saved in " + this.destinationPath + "."); 
		
		// get input stream (from internet)
		try {
			is = new URL(this.source).openStream();
			bos = new BufferedOutputStream(new FileOutputStream(output));
			
			for(int c; (c = is.read()) != -1 ;){
				bos.write(c);
			}
			log.info("Loading successfully.");
			
			if(bos != null){
				bos.flush();
				bos.close();
			}
			if (is != null) is.close();
			
			return output;
			
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			// There was a problem getting the (remote) file, just show the error as information for the user
			log.error(e1.toString() + ". May not be fatal." ) ;
			isInternetStream = Boolean.FALSE;
			output = null;	// set to null
		}

		// if no internet connection, trying to load xsd schema locally
		if(! isInternetStream){
			
			// This should work for jar files (dtd folder located in root)
			log.info("Trying to access local dtd folder at standard location " + InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR + " ...");
			File dtdFile = new File( InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR );
			
			if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
				log.info("Using the local DTD " + dtdFile.getAbsolutePath());
				output = new File( dtdFile.getAbsolutePath() );
				// URL localUrl = this.getClass().getResource(Constants.MATSIM_4_URBANSIM_XSD_LOCAL_CURRENT);
				// output = new File( localUrl.getPath() );
				return output; // return path to local xsd file
			}
			
			{
				// This works in Eclipse environment for debugging ...
				String currentDir = System.getProperty("user.dir");
				int index = (currentDir.indexOf("playground") > 0) ? currentDir.indexOf("playground") : currentDir.indexOf("contrib");
				String root = currentDir.substring(0, index);

				dtdFile = new File( root + "/matsim" + InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR );
				log.info("Trying to access local dtd folder at standard location " + dtdFile.getAbsolutePath() + " ...");

				if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
					log.info("Using the local DTD " + dtdFile.getAbsolutePath());
					output = new File( dtdFile.getAbsolutePath() );
					// URL localUrl = this.getClass().getResource(Constants.MATSIM_4_URBANSIM_XSD_LOCAL_CURRENT);
					// output = new File( localUrl.getPath() );
					return output; // return path to local xsd file
				}
			}

			{
				// This works for me (kai) for command line calling ...
				String currentDir = System.getProperty("user.dir");
				int index = (currentDir.indexOf("playground") > 0) ? currentDir.indexOf("playground") : currentDir.indexOf("contrib");
				String root = currentDir.substring(0, index);

				dtdFile = new File( root + "/matsim/src/main/resources/" + InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR );
				log.info("Trying to access local dtd folder at standard location " + dtdFile.getAbsolutePath() + " ...");

				if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
					log.info("Using the local DTD " + dtdFile.getAbsolutePath());
					output = new File( dtdFile.getAbsolutePath() );
					// URL localUrl = this.getClass().getResource(Constants.MATSIM_4_URBANSIM_XSD_LOCAL_CURRENT);
					// output = new File( localUrl.getPath() );
					return output; // return path to local xsd file
				}			
			}
		}
		
		// could neither get the remote nor the local version of the xsd
		System.out.flush();
		log.warn("Could neither get the XSD from the web nor a local one.");
		System.err.flush() ;
		return null;
	}
}

