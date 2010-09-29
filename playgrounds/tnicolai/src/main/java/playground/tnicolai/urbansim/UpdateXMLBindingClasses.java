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

import com.sun.tools.xjc.XJCFacade;

import playground.tnicolai.urbansim.constants.Constants;
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
	
	private static String helpMessage = null;
	
	private static String targetPackage = null;
	private static String outputDirectory = null;
	private static String xsdLocation = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// set help message
		helpMessage = "Please run this program as described below:\n" +
					  "java UpdateXMLBindingClasses --jaxbLocation=my path/libs/jaxb-2.1.7/bin/xjc.sh\n" +
					  "Optional you can define:\n"+
					  "- the desired location of the generated binding classes: --destination=your/destination/path\n" +
					  "- the desierd package structure: --package=your.desired.package.structure\n";
					  
		
		// script to create jaxb bindings
		log.info("Start generating xml bindings ...");
		log.info("");
		
		if(!checkParameter( args ))
			System.exit(0);
		
		log.info("");
		log.info("Parameter overview");
		log.info("Target package: " + targetPackage);
		log.info("Destination directory: " + outputDirectory);
		log.info("XSD location: " + xsdLocation);
		log.info("");
		
		String[] arg = new String[] { "-p", targetPackage, "-d", outputDirectory, xsdLocation };
		
		try {
			XJCFacade.main(arg);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		
		// old version
//		String cmd = "java -jar " + jaxBLocation + " -p " + targetPackage + " -d " + tmpDirectory + " " + xsdLocation;
//		try{
//			log.info("Running command: " + cmd );
//
//			// executing JAXB ...
//			Runtime rt = Runtime.getRuntime();
//            Process proc = rt.exec( cmd );
//            proc.waitFor();
//            int exitVal = proc.exitValue();
//            // ... and here the execution ends
//            
//            if( exitVal == 0){
//            	log.info("Runnung command successful!");
//				// copy generated files into destination directory
//				String source = tmpDirectory + outputPackage.replace(".", File.separator);
//				log.info("Copying generated files from " + source + " to " + outputDirectory);
//				FileCopy.copyTree(source, outputDirectory);
//            }
//			TempDirectoryUtil.deleteDirectory( tmpDirectory );
//		}
//		catch (IOException e) {
//			log.error("Error occoured executing command: " + cmd );
//			e.printStackTrace();
//		}
//		catch (InterruptedException ie) {
//			ie.printStackTrace();
//		}
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
			if ( parts[0].equals("--xsdLocation") ) {
				xsdLocation = parts[1];
			}
			else if ( parts[0].equals("--destination") ) {
				outputDirectory = parts[1];
			}
			else if ( parts[0].equals("--package") ) {
				targetPackage = parts[1];
			}
			else if ( parts[0].equals("--help") )
				log.info(helpMessage);
		}
		
		
		if(xsdLocation == null){
			log.warn("XSD location not given (optional)...");
			String tmpDir = TempDirectoryUtil.createCustomTempDirectory("xsd");
			// set default location			
			LoadFile loadFile = new LoadFile(Constants.MATSim_4_UrbanSim_XSD, tmpDir, "MATSim4UrbanSimConfigSchema.xsd");
			xsdLocation = loadFile.loadMATSim4UrbanSimXSDString();
			
			log.warn("Set xsd default location to: " + xsdLocation);
		}
		if(outputDirectory == null){
			log.warn("Destination not given (optional)...");
			// set default location
			outputDirectory = Constants.MATSIM_WORKING_DIRECTORY + "/tnicolai/src/main/java/";
			TempDirectoryUtil.createDirectory( outputDirectory );
			log.warn("Set default destination to: " + outputDirectory);
		}
		if(targetPackage == null){
			log.warn("Package name not given (optional)...");
			// set default location
			targetPackage = "playground.tnicolai.urbansim.com.matsim.config";
			log.warn("Set default package name to: " + targetPackage);
		}

		if( !(outputDirectory!= null && 
			  targetPackage != null && 
			  isValidLocataion(xsdLocation)) )
		{
			log.warn(helpMessage);
			return false;
		}
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

//class JAXBRun extends Thread{
//	
//	private String targetPackage = null;
//	private String tmpDirectory = null;
//	private String xsdLocation = null;
//	
//	public JAXBRun(String targetPackage, String tmpDirectory, String xsdLocation){
//		this.targetPackage = targetPackage;
//		this.tmpDirectory = tmpDirectory;
//		this.xsdLocation = xsdLocation;
//	}
//	
//	public void run(){
//		
//		String[] arg = new String[] { "-p", targetPackage, "-d", tmpDirectory, xsdLocation };
//		
//		try {
//			XJCFacade.main(arg);
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//	}
//	
//}

