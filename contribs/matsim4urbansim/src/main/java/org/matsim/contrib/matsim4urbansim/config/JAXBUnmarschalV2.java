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
package org.matsim.contrib.matsim4urbansim.config;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.ObjectFactory;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;


/**
 * @author thomas
 *
 */
public class JAXBUnmarschalV2 extends MatsimJaxbXmlParser{
	
	// logger
	private static final Logger log = Logger.getLogger(JAXBUnmarschalV2.class);
	
	private String matsimConfigFile = null;
	
	/**
	 * default constructor
	 * @param configFile
	 */
	public JAXBUnmarschalV2(String configFile){
		// schemaLocation
		super(InternalConstants.V2_MATSIM_4_URBANSIM_XSD_MATSIMORG);
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
		
		log.info("Unmaschalling MATSim configuration from: " + matsimConfigFile );
		log.info("...");
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();

			// validate file
			super.validateFile(this.matsimConfigFile, unmarschaller);
			
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
		}

		log.info("... finished unmarschallig");
		// return initialized object representation of matsim4urbansim config file
		return matsimConfig;
	}
	
	private void isFileAvailable(File file){
		if(!file.exists()){
			log.error(matsimConfigFile + " not found!!!");
			System.exit(-1);
		}
	}
	
	@Override
	public void readJaxbFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		throw new UnsupportedOperationException("Use unmaschalMATSimConfig()");
	}

}

