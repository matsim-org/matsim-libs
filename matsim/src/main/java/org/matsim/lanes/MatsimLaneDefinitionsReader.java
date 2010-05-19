/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimLaneDefinitionReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.lanes;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class MatsimLaneDefinitionsReader {
	
	private static final Logger log = Logger
			.getLogger(MatsimLaneDefinitionsReader.class);
	
	public static final String SCHEMALOCATIONV11 = "http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd";

	public static final String SCHEMALOCATIONV20 = "http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd";
	
	private LaneDefinitions laneDefinitions;
	
	private String lastReadFileFormat = null;

	public MatsimLaneDefinitionsReader(LaneDefinitions laneDefs) {
		this.laneDefinitions = laneDefs;
	}

	public void readFile(final String filename) {
		this.lastReadFileFormat = null;
		try {
			MatsimFileTypeGuesser fileTypeGuesser = new MatsimFileTypeGuesser(filename);
			String sid = fileTypeGuesser.getSystemId();
			MatsimJaxbXmlParser reader = null;
			if (sid != null) {
				log.debug("creating parser for system id: " + sid);
				this.lastReadFileFormat = sid;
				if (sid.compareTo(SCHEMALOCATIONV11) == 0) {
					reader = new LaneDefinitionsReader11(this.laneDefinitions, sid);
					log.info("Using LaneDefinitionReader11...");
					log.warn("The laneDefinitions_v1.1.xsd file format is used. For the use within the mobility simulation it is strongly recommended to" +
							"convert the read data to the v2.0.xsd format using the LaneDefinitionsV11ToV20Conversion class. If the data is read by the " +
							"Controler or ScenarioLoader this will be done automatically and noticed by a separate message.");
				}
				else if (sid.compareTo(SCHEMALOCATIONV20) == 0){
					reader = new LaneDefinitionsReader20(this.laneDefinitions, sid);
					log.info("Using LaneDefinitionReader20...");
				}
				else {
					throw new RuntimeException("Unsupported file format: " + sid);
				}
			}
			else {
				log.error(MatsimFileTypeGuesser.SYSTEMIDNOTFOUNDMESSAGE);
				throw new IllegalArgumentException(MatsimFileTypeGuesser.SYSTEMIDNOTFOUNDMESSAGE);
			}
			log.info("reading file " + filename);
			reader.readFile(filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the schema location of the last file read by this reader.
	 */
	public String getLastReadFileFormat() {
		return lastReadFileFormat;
	}

}
