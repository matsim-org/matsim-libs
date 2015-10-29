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
package org.matsim.lanes.data.v20;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author dgrether
 */
public class LaneDefinitionsReader implements MatsimSomeReader {
	
	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader.class);
	
	public static final String SCHEMALOCATIONV11 = "http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd";

	public static final String SCHEMALOCATIONV20 = "http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd";
	
	private Lanes laneDefinitions;

	public LaneDefinitionsReader(Scenario scenario) {
		this.laneDefinitions = scenario.getLanes();
	}


	/**
	 * Reads both file formats, 1.1 and 2.0.
	 */
	public void readFile(final String filename) {
		try {
			MatsimFileTypeGuesser fileTypeGuesser = new MatsimFileTypeGuesser(filename);
			String sid = fileTypeGuesser.getSystemId();
			MatsimJaxbXmlParser reader;
			if (sid != null) {
				log.debug("creating parser for system id: " + sid);
				if (sid.compareTo(SCHEMALOCATIONV11) == 0) {
					log.info("Using LaneDefinitionReader11...");
					throw new RuntimeException("The laneDefinitions_v1.1.xsd file format is used. For the use within the mobility simulation it is strongly recommended to" +
							"convert the read data to the v2.0.xsd format using the LaneDefinitionsV11ToV20Conversion class. " +
							"With the 0.5 release of MATSim the automatic conversion is switched off. Simulation will not run if the 1.1 file format" +
							"is given as input. Please convert manually.");
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
		} catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
			throw new RuntimeException(e);
		}

	}


}
