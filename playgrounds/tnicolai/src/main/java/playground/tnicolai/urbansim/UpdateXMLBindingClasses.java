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
import playground.tnicolai.urbansim.utils.CommonUtilities;
import playground.tnicolai.urbansim.utils.io.FileCopy;
import playground.tnicolai.urbansim.utils.io.LoadFile;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;

/**
 * @author thomas
 * 
 * This class generates a set of Java classes via JAXB that represents a given schema (xsd file) and copies them into the package "com.matsim.config".
 * The JAXB executables are located outside the playground in "OPUS_HOME/libs".
 * 
 */
public class UpdateXMLBindingClasses {
	
	private static final Logger log = Logger.getLogger(UpdateXMLBindingClasses.class);
	
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
		
		String cmd = "java -jar " + jaxBLocation + " -p " + outputPackage + " -d " + tmpDirectory + " " + xsdLocation;
		
		try{
			log.info("Running command: " + cmd );

			// executing JAXB ...
			Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec( cmd );
            proc.waitFor();
            int exitVal = proc.exitValue();
            // ... and here the execution ends
            
            if( exitVal == 0){
            	log.info("Runnung command successful!");
				// copy generated files into destination directory
				String source = tmpDirectory + outputPackage.replace(".", File.separator);
				log.info("Copying generated files from " + source + " to " + outputDirectory);
				FileCopy.copyTree(source, outputDirectory);
            }
			TempDirectoryUtil.deleteDirectory( tmpDirectory );
		}
		catch (IOException e) {
			log.error("Error occoured executing command: " + cmd );
			e.printStackTrace();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		log.info("Successful finished creating xml bindings ...");
	}
	
	/**
	 * checks correctness of input parameters
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
			else if ( parts[0].equals("--package") ) {
				outputPackage = parts[1];
			}
			else if ( parts[0].equals("--help") ) {
				log.info("Enter the location of the JAXB libary and the location of the schema file (xsd) as described below:");
				log.info("java UpdateXMLParser --jaxbLocation=[path/to/your/jaxb/libar] --xsdLocation=[path/to/your/xsd.file]");
			}
		}
		
		tmpDirectory = CommonUtilities.getCurrentPath(UpdateXMLBindingClasses.class) + "tmp/"; // Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY;
		boolean dirExsists = TempDirectoryUtil.createDirectory( tmpDirectory );
		
		if(jaxBLocation == null){
			log.info("JAXB libary not given...");
			// set default location
			jaxBLocation = Constants.OPUS_HOME + "/libs/jaxb-2.1.7/bin/xjc.sh";
			log.info("Set default location to: " + jaxBLocation);
		}
		if(xsdLocation == null){
			log.info("XSD location not given...");
			// set default location			
			LoadFile loadFile = new LoadFile(Constants.MATSim_4_UrbanSim_XSD, tmpDirectory, "MATSim4UrbanSimConfigSchema.xsd");
			xsdLocation = loadFile.loadMATSim4UrbanSimXSDString();
			
			log.info("Set xsd default location to: " + xsdLocation);
		}
		if(outputDirectory == null){
			log.info("Destination not given...");
			// set default location
			outputDirectory = Constants.MATSIM_WORKING_DIRECTORY + "/tnicolai/src/main/java/playground/tnicolai/urbansim/com/matsim/config";
			log.info("Set default destination to: " + outputDirectory);
		}
		if(outputPackage == null){
			log.info("Package name not given...");
			// set default location
			outputPackage = "playground.tnicolai.urbansim.com.matsim.config";
			log.info("Set default package name to: " + outputPackage);
		}

		if( !(outputDirectory!= null && 
			  outputPackage != null && 
			  isValidLocataion(jaxBLocation) && 
			  isValidLocataion(xsdLocation) && 
			  tmpDirectory != null && 
			  dirExsists) )
			return false;
		return true;
	}
	
	/**
	 * determines if a path wether exists or not
	 * @return true if path exists otherwise false
	 */
	private static boolean isValidLocataion(String parameter){
		
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

