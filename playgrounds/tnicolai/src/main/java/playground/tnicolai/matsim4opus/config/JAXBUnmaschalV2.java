/* *********************************************************************** *
 * project: org.matsim.*
 * JAXBUnmaschal.java
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
package playground.tnicolai.matsim4opus.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.ObjectFactory;
import playground.tnicolai.matsim4opus.utils.io.LoadFile;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;

/**
 * @author thomas
 *
 */
public class JAXBUnmaschalV2{
	
	// logger
	private static final Logger log = Logger.getLogger(JAXBUnmaschalV2.class);
	
	private String matsimConfigFile = null;
	
	/**
	 * default constructor
	 * @param configFile
	 */
	public JAXBUnmaschalV2(String configFile){
		this.matsimConfigFile = configFile;
	}
	
	
	/**
	 * unmarschal (read) matsim config
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public MatsimConfigType unmaschalMATSimConfig(){
		
		// Java representation of the schema file.
		MatsimConfigType matsimConfig = null;
		
		log.info("Staring unmaschalling MATSim configuration from: " + matsimConfigFile );
		log.info("...");
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();

			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// ... and initialize it with an xsd

			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			String tempDir = TempDirectoryUtil.createCustomTempDirectory("tmp");
			LoadFile loadFile = new LoadFile(InternalConstants.V2_MATSIM_4_URBANSIM_XSD_MATSIMORG , tempDir , InternalConstants.V2_XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			// tnicolai: debugging
			//file2XSD = new File("/Users/thomas/Development/workspace/matsim/src/main/resources/dtd/matsim4urbansim_v2.xsd");
			//log.warn("Running Jaxb in debugging mode. Change this!");
			
			if(file2XSD == null || !file2XSD.exists())
				return null;
			
			printOutXSD(file2XSD);
			
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			// set the schema for validation while reading/importing the MATSim xml config.
			unmarschaller.setSchema(schema);
			
			File inputFile = new File( matsimConfigFile );
			isFileAvailable(inputFile);
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// The structure of both objects must match.
			if(object.getClass() == MatsimConfigType.class)
				matsimConfig = (MatsimConfigType) object;
			else
				matsimConfig = (( JAXBElement<MatsimConfigType>) object).getValue();
			
		} catch(JAXBException je){
			je.printStackTrace();
			return null;
		} catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		} finally{
			// clean up temp files
			TempDirectoryUtil.cleaningUpCustomTempDirectories();
		}

		log.info("... finished unmarschallig");
		// return initialized object representation of matsim4urbansim config file
		return matsimConfig;
	}
	
	/**
	 * prints out loaded xsd
	 * @param file2XSD
	 */
	private void printOutXSD(File file2XSD){
		
		if(file2XSD != null && file2XSD.exists()){
			try {
				BufferedReader br = IOUtils.getBufferedReader(file2XSD.getCanonicalPath());
				String line = null;
				while( (line = br.readLine()) != null)
					log.info( line );
			} catch (UncheckedIOException e) {
				// no warning needed here
			} catch (IOException e) {
				// no warning needed here
			}
		}
	}
	
	private void isFileAvailable(File file){
		if(!file.exists()){
			log.error(matsimConfigFile + " not found!!!");
			System.exit(-1);
		}
	}

}

