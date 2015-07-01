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
package org.matsim.lanes.data.v20;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.lanedefinitions20.ObjectFactory;
import org.matsim.jaxb.lanedefinitions20.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions20.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions20.XMLLaneType;
import org.matsim.jaxb.lanedefinitions20.XMLLanesToLinkAssignmentType;
import org.xml.sax.SAXException;


/**
 * Reader for the http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd
 * file format.
 * @author dgrether
 *
 */
class LaneDefinitionsReader20 extends MatsimJaxbXmlParser implements MatsimSomeReader {

	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader20.class);

	private LaneDefinitions20 laneDefinitions;

	private LaneDefinitionsFactory20 builder;
	/**
	 * @param schemaLocation
	 */
	public LaneDefinitionsReader20(LaneDefinitions20 laneDefs, String schemaLocation) {
		super(schemaLocation);
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getFactory();
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
					.newInstance(org.matsim.jaxb.lanedefinitions20.ObjectFactory.class);
			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			super.validateFile(filename, u);
			log.info("starting unmarshalling " + filename);
			InputStream stream = null;
			try {
			  stream = IOUtils.getInputStream(filename);
				xmlLaneDefinitions = (XMLLaneDefinitions) u.unmarshal(stream);
			}
			finally {
				try {
					if (stream != null) { stream.close();	}
				} catch (IOException e) {
					log.warn("Could not close stream.", e);
				}
			}
			
			//convert the parsed xml-instances to basic instances
			LanesToLinkAssignment20 l2lAssignment;
			Lane lane = null;
			for (XMLLanesToLinkAssignmentType lldef : xmlLaneDefinitions
					.getLanesToLinkAssignment()) {
				l2lAssignment = builder.createLanesToLinkAssignment(Id.create(lldef
						.getLinkIdRef(), Link.class));
				for (XMLLaneType laneType : lldef.getLane()) {
					lane = builder.createLane(Id.create(laneType.getId(), Lane.class));
					
					if (!laneType.getLeadsTo().getToLane().isEmpty()) {
						for (XMLIdRefType toLaneId : laneType.getLeadsTo().getToLane()){
							lane.addToLaneId(Id.create(toLaneId.getRefId(), Lane.class));
						}
					}
					else if (!laneType.getLeadsTo().getToLink().isEmpty()){
						for (XMLIdRefType toLinkId : laneType.getLeadsTo().getToLink()){
							lane.addToLinkId(Id.create(toLinkId.getRefId(), Link.class));
						}
					}
					
					if (laneType.getCapacity() == null){
						log.warn("Capacity not set in lane definition, using default...");
						laneType.setCapacity(fac.createXMLLaneTypeXMLCapacity());
					}
					lane.setCapacityVehiclesPerHour(laneType.getCapacity().getVehiclesPerHour());
					
					if (laneType.getRepresentedLanes() == null) {
						laneType.setRepresentedLanes(fac
								.createXMLLaneTypeXMLRepresentedLanes());
					}
					lane.setNumberOfRepresentedLanes(laneType.getRepresentedLanes()
							.getNumber());
					
					if (laneType.getStartsAt() == null) {
						laneType.setStartsAt(fac.createXMLLaneTypeXMLStartsAt());
					}
					lane.setStartsAtMeterFromLinkEnd(laneType.getStartsAt().getMeterFromLinkEnd());
					
					lane.setAlignment(laneType.getAlignment());
					
					l2lAssignment.addLane(lane);
				}
				this.laneDefinitions.addLanesToLinkAssignment(l2lAssignment);
			}
	}

}
