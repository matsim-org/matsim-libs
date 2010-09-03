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
package playground.tnicolai.urbansim.utils.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.utils.CommonUtilities;

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
		this.destinationPath = CommonUtilities.checkPathEnding( destination );
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
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		
		log.info("Trying to load " + this.source + ". In some cases (e.g. network interface up but no connection), this may take a bit.");
		log.info("The xsd file will be saved in " + this.destinationPath + "."); 
		
		try{
			// input streams
			is = new URL(this.source).openStream();
			bis = new BufferedInputStream(is);
			
			// output
			File dir = new File(this.destinationPath);
			if(!dir.exists())
				dir.mkdirs();
			
			File output = new File(destinationPath + fileName);
			
			bos = new BufferedOutputStream(new FileOutputStream(output));
			
			for(int c; (c = is.read()) != -1 ;){
				bos.write(c);
			}
			log.info("Loading successfully.");
			
			if(bos != null){
				bos.flush();
				bos.close();
			}
			if (bis != null) bis.close();
			if (is != null) is.close();
			
			return output;
		}
		catch(IOException io){
			io.printStackTrace();
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
}

