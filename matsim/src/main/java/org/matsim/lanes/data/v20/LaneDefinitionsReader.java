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

import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class LaneDefinitionsReader implements MatsimReader {
	
	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader.class);
	
	public static final String SCHEMALOCATIONV11 = "http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd";

	public static final String SCHEMALOCATIONV20 = "http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd";
	
	private Lanes laneDefinitions;

	public LaneDefinitionsReader(Scenario scenario) {
		this.laneDefinitions = scenario.getLanes();
	}


	@Override
	public void readFile(final String filename) {
		try {
			LaneDefinitionsReader20 delegate = new LaneDefinitionsReader20(this.laneDefinitions);
			log.info("reading file " + filename);
			delegate.readFile(filename);
		} catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void readURL(final URL url) {
		try {
			LaneDefinitionsReader20 delegate = new LaneDefinitionsReader20(this.laneDefinitions);
			log.info("reading file " + url.toString());
			delegate.readURL(url);
		} catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
			throw new RuntimeException(e);
		}

	}



}
