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
package playground.tnicolai.urbansim.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.MATSim4Urbansim;

/**
 * @author thomas
 *
 */
public class LoadFile {
	
	// logger
	private static final Logger log = Logger.getLogger(LoadFile.class);
	
	private String source;
	private String destination;
	
	@SuppressWarnings(value = "all")
	private LoadFile(){
		// deactivated default constructor
	}
	
	/**
	 * constructor
	 * @param source
	 * @param destination
	 */
	public LoadFile(String source, String destination){
		this.source = source;
		this.destination = destination;
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
		log.info("The xsd file will be saved in " + this.destination + "."); 
		
		try{
			// input streams
			is = new URL(this.source).openStream();
			bis = new BufferedInputStream(is);
			
			// output
			File output = new File(this.destination);
			bos = new BufferedOutputStream(new FileOutputStream(output));
			
			for(int c; (c = is.read()) != -1 ;){
				bos.write(c);
			}
			log.info("Loading successfully.");
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
		finally{
			try{
				if(bos != null){
					bos.flush();
					bos.close();
				}
				if (bis != null) bis.close();
				if (is != null) is.close();
			} catch(Exception e){e.printStackTrace();}
		}
	}
	
	
}

