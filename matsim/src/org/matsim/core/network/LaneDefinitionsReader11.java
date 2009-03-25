/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimLaneReader
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
package org.matsim.core.network;

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.network.BasicLane;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.network.BasicLaneDefinitionsBuilder;
import org.matsim.core.basic.network.BasicLanesToLinkAssignment;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.lanedefinitions11.ObjectFactory;
import org.matsim.jaxb.lanedefinitions11.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions11.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions11.XMLLaneType;
import org.matsim.jaxb.lanedefinitions11.XMLLanesToLinkAssignmentType;
import org.xml.sax.SAXException;


/**
 * Reader for the http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd
 * file format.
 * @author dgrether
 *
 */
public class LaneDefinitionsReader11 extends MatsimJaxbXmlParser {
	
	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader11.class);
	
	private BasicLaneDefinitions laneDefinitions;
	
	private BasicLaneDefinitionsBuilder builder;
	/**
	 * @param schemaLocation
	 */
	public LaneDefinitionsReader11(BasicLaneDefinitions laneDefs, String schemaLocation) {
		super(schemaLocation);
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getLaneDefinitionBuilder();
	}

	/**
	 * @see org.matsim.core.utils.io.MatsimJaxbXmlParser#readFile(java.lang.String)
	 */
	@Override
	public void readFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		//create jaxb infrastructure
		JAXBContext jc;
		XMLLaneDefinitions xmlLaneDefinitions;
			jc = JAXBContext
					.newInstance(org.matsim.jaxb.lanedefinitions11.ObjectFactory.class);
			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			super.validateFile(filename, u);
			log.info("starting unmarshalling " + filename);
			xmlLaneDefinitions = (XMLLaneDefinitions) u
					.unmarshal(new FileInputStream(filename));

			//convert the parsed xml-instances to basic instances
			BasicLanesToLinkAssignment l2lAssignment;
			BasicLane lane = null;
			for (XMLLanesToLinkAssignmentType lldef : xmlLaneDefinitions
					.getLanesToLinkAssignment()) {
				l2lAssignment = builder.createLanesToLinkAssignment(new IdImpl(lldef
						.getLinkIdRef()));
				for (XMLLaneType laneType : lldef.getLane()) {
					lane = builder.createLane(new IdImpl(laneType.getId()));
					for (XMLIdRefType toLinkId : laneType.getToLink()) {
						lane.addToLinkId(new IdImpl(toLinkId.getRefId()));
					}
					if (laneType.getRepresentedLanes() == null) {
						laneType.setRepresentedLanes(fac
								.createXMLLaneTypeXMLRepresentedLanes());
					}
					lane.setNumberOfRepresentedLanes(laneType.getRepresentedLanes()
							.getNumber());
					if (laneType.getLength() == null) {
						laneType.setLength(fac.createXMLLaneTypeXMLLength());
					}
					lane.setLength(laneType.getLength().getMeter());
					l2lAssignment.addLane(lane);
				}
				this.laneDefinitions.addLanesToLinkAssignment(l2lAssignment);
			}
	}

}
