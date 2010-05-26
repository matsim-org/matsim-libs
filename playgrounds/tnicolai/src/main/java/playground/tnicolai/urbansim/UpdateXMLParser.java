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
import playground.tnicolai.urbansim.utils.io.FileCopy;

/**
 * @author thomas
 * 
 * This class generates a set of Java classes via JAXB that represents a given schema (xsd file) and copies them into the package "com.matsim.config".
 * The JAXB executables are located outside the playground in "OPUS_HOME/libs".
 * 
 */
public class UpdateXMLParser {
	
	private static final Logger log = Logger.getLogger(UpdateXMLParser.class);
	
	private static String jaxBLocation = null;
	private static String outputPackage = null;
	private static String outputDirectory = null;
	private static String tmpDirectory = null;
	private static String xsdLocation = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// script to create jaxb bindings
		log.info("Start generating xml bindings ...");
		log.info("");
		
		if(!checkParameter( args ))
			System.exit(0);
		
		log.info("");
		log.info("Parameter overview");
		log.info("JaxB location: " + jaxBLocation);
		log.info("Target package: " + outputPackage);
		log.info("Temp directory: " + tmpDirectory);
		log.info("Destination directory: " + outputDirectory);
		log.info("XSD location: " + xsdLocation);
		log.info("");
		
		String cmd = jaxBLocation + " -p " + outputPackage + " -d " + tmpDirectory + " " + xsdLocation;
		
		try{
			log.info("Running command: " + cmd );
			Runtime.getRuntime().exec( cmd );
			// copy generated files into destination directory
			String source = tmpDirectory + File.separator + outputPackage;
			log.info("Copying generated files from " + source + " to " + outputDirectory);
			FileCopy.copyTree(source, outputDirectory);
		}
		catch (IOException e) {
			log.error("Error occoured executing command: " + cmd );
			e.printStackTrace();
		}
		log.info("Successful finished creating xml bindings ...");
	}
	
	/**
	 * 
	 * @param args
	 * @return
	 */
	private static boolean checkParameter(String args[]){
		
		for ( int i=0 ; i<args.length ; i++ ) {
			log.info( "Parameter: " + i + " = " + args[i] ) ;
			String[] parts = args[i].split("=");
			if ( parts[0].equals("--jaxbLocation") ) {
				jaxBLocation = parts[1];
			} else if ( parts[0].equals("--xsdLocation") ) {
				xsdLocation = parts[1];
			}
			else if ( parts[0].equals("--destination") ) {
				outputDirectory = parts[1];
			}
			else if ( parts[0].equals("--help") ) {
				log.info("Enter the location of the JAXB libary and the location of the schema file (xsd) as described below:");
				log.info("java UpdateXMLParser --jaxbLocation=[path/to/your/jaxb/libar] --xsdLocation=[path/to/your/xsd.file]");
			}
		}
		
		if(jaxBLocation == null){
			log.info("JAXB libary not given...");
			// set default location
			jaxBLocation = Constants.OPUS_HOME + "/libs/jaxb-2.1.7/bin/xjc.sh";
			log.info("Set default location to: " + jaxBLocation);
		}
		if(xsdLocation == null){
			log.info("XSD location not given...");
			// set default location
			xsdLocation = Constants.PYTHONPATH + "/opus_matsim/sustain_city/models/pyxb_xml_parser/MATSim4UrbanSimConfigSchema.xsd";
			log.info("Set default location to: " + xsdLocation);
		}
		if(outputDirectory == null){
			log.info("Destination not given...");
			// set default location
			outputDirectory = Constants.MATSIM_WORKING_DIRECTORY + "/tnicolai/src/main/java/playground/tnicolai/urbansim/com/matsim/config";
			log.info("Set default destination to: " + outputDirectory);
		}

		outputPackage = "generatedSchemaBindings";
		tmpDirectory = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY;
		
		
		if( !(outputDirectory!= null && outputPackage != null && checkParameter(jaxBLocation) && checkParameter(xsdLocation) && tmpDirectory != null) )
			return false;
		return true;
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

