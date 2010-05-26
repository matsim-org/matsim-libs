/* *********************************************************************** *
 * project: org.matsim.*
 * UpdateXMLParser.java
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
package playground.tnicolai.urbansim;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class UpdateXMLParser {
	
	private static final Logger log = Logger.getLogger(UpdateXMLParser.class);
	
	private static String jaxBLocation = null;
	private static String outputPackage = null;
	private static String outputDirectory = null;
	private static String xsdLocation = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// script to create jaxb bindings
		log.info("Start generating xml bindings ...");
		log.info("");

		jaxBLocation = Constants.MATSIM_WORKING_DIRECTORY + "/" + Constants.MATSIM_PLAYGROUND_DIRECTORY_STATIC + "/libs/jaxb-2.1.7/bin/xjc.sh";
		outputPackage = "playground.tnicolai.urbansim.com.matsim.config";
		outputDirectory = Constants.MATSIM_PLAYGROUND_DIRECTORY_STATIC;
		xsdLocation = Constants.PYTHONPATH + "/opus_matsim/sustain_city/models/pyxb_xml_parser/MATSim4UrbanSimConfigSchema.xsd";
		
		if( !(outputDirectory!= null && outputPackage != null && checkParameter(jaxBLocation) && checkParameter(xsdLocation)) )
			System.exit(0);
		
		log.info("Parameter overview");
		log.info("JaxB location: " + jaxBLocation);
		log.info("Target package: " + outputPackage);
		log.info("Generated files will go into this directory: " + outputDirectory);
		log.info("XSD location: " + xsdLocation);
		log.info("");
		
		String cmd = jaxBLocation + " -p " + outputPackage + " -d " + outputDirectory + " " + xsdLocation;
		
		try{
			log.info("Running command: " + cmd );
			Runtime.getRuntime().exec( cmd );
		}
		catch (IOException e) {
			log.error("Error occoured executing command: " + cmd );
			e.printStackTrace();
		}
		log.info("Successful finished creating xml bindings ...");
	}
	
	/**
	 * 
	 * @return
	 */
	private static boolean checkParameter(String parameter){
		
		try{
			if(parameter != null){
				File f = new File(parameter);
				if( !(f.exists()) ){
					log.error("Location not found! " + f.getCanonicalPath());
					return false;
				}
			}
			else{
				log.error("Location not initialized!");
				return false;
			}
		}
		catch(IOException e){}
		return true;
	}
}

