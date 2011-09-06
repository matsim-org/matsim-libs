/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimProperies.java
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
package playground.tnicolai.urbansim.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.constants.Constants;


/**
 * @author thomas
 *
 */
public class MATSimProperties extends Properties{
	
	public static final long serialVersionUID 	= 1L;
	
	public static MATSimProperties properies		= null;
	
	// logger
	private static final Logger log = Logger.getLogger(MATSimProperties.class);
	
	static{
		properies = new MATSimProperties();
	}
	
	/**
	 * constructor
	 * access to resources only in static way
	 */
	private MATSimProperties(){	
		
		String matsimProperiesFile = Constants.MATSIM_4_OPUS_CONFIG + Constants.MATSIM_PROPERTIES_FILE;
		File propertiesFile = new File(matsimProperiesFile);
		
		try{
			
			if(!propertiesFile.exists())
				propertiesFile.createNewFile();
		
			this.load(new FileReader(propertiesFile));
		}
		catch(FileNotFoundException fnf){
			log.error("Properties file " + matsimProperiesFile + " not found! SHUTDOWN MATSim !!!");
			System.exit(Constants.MATSIM_PROPERTIES_FILE_NOT_FOUND);
		}
		catch (IOException io) {
			io.printStackTrace();
			log.error("SHUTDOWN MATSim !!!");
			System.exit(Constants.EXCEPTION_OCCURED);
		}
	}
	
	/**
	 * save new MATSim state in properties file
	 */
	public void saveMATSimState(){
		
		try{
			this.store(new FileOutputStream(Constants.MATSIM_4_OPUS_CONFIG + "/matsim.properties", false), "MATSim state file");
		}
		catch(IOException io){
			io.printStackTrace();
		}
	}

}

