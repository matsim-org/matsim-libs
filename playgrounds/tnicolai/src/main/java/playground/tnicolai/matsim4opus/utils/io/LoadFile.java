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
package playground.tnicolai.matsim4opus.utils.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;


/**
 * @author thomas
 *
 */
public class LoadFile {
	
	// logger
	private static final Logger log = Logger.getLogger(LoadFile.class);
	
	private String source 			= null;
	private String destinationPath	= null;
	private String fileName 		= null;
	
	@SuppressWarnings(value = "all")
	private LoadFile(){
		// deactivated default constructor
	}
	
	/**
	 * constructor
	 * @param source
	 * @param destination
	 */
	public LoadFile(String source, String destination, String fileName){
		this.source = source;
		this.destinationPath = UtilityCollection.checkPathEnding( destination );
		this.fileName = fileName;
	}
	
	/**
	 * linke loadMATSim4UrbanSimXSD() but returns the canonical path
	 * of the loaded xsd file
	 * @return path of loaded xsd file
	 */
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
		
		// if no internet connection, trying to load xsd schema locally from matsim/dtd/ directory
		if(! isInternetStream){
			log.info("Trying to access local dtd folder at standard location ./dtd...");
			File dtdFile = new File( Constants.MATSIM_4_URBANSIM_XSD_LOCAL );
			log.debug("dtdfile: " + dtdFile.getAbsolutePath());
			URL localUrl = this.getClass().getResource(Constants.MATSIM_4_URBANSIM_XSD_LOCAL);
			System.out.println( localUrl.getPath() );
			output = new File( localUrl.getPath() );
			log.info("Found local xsd at: " + localUrl.getPath() );
		}

		// return path to local xsd file
		return output;
	}
	
}

