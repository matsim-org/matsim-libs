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
package org.matsim.lanes.data.v11;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.jaxb.lanedefinitions11.ObjectFactory;
import org.matsim.jaxb.lanedefinitions11.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions11.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions11.XMLLaneType;
import org.matsim.jaxb.lanedefinitions11.XMLLanesToLinkAssignmentType;
import org.matsim.lanes.data.Lane;
import org.xml.sax.SAXException;


/**
 * Reader for the http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd
 * file format.
 * @author dgrether
 *
 */
@Deprecated
public class LaneDefinitionsReader11 implements MatsimReader {

	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader11.class);

	private LaneDefinitions11 laneDefinitions;

	private LaneDefinitionsFactory11 builder;
	/**
	 * @deprecated use constructor without schema location
	 */
	@Deprecated
	public LaneDefinitionsReader11(LaneDefinitions11 laneDefs, String schemaLocation) {
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getFactory();
	}

	public LaneDefinitionsReader11(LaneDefinitions11 laneDefs) {
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getFactory();
	}

	@Override
	public void readFile(String filename) throws RuntimeException {
		//create jaxb infrastructure
		JAXBContext jc;
		XMLLaneDefinitions xmlLaneDefinitions;
		try {
			jc = JAXBContext
					.newInstance(org.matsim.jaxb.lanedefinitions11.ObjectFactory.class);
			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/laneDefinitions_v1.1.xsd")));
			log.info("starting unmarshalling " + filename);

			try (InputStream stream = IOUtils.getInputStream(filename)) {
				xmlLaneDefinitions = (XMLLaneDefinitions) u.unmarshal(stream);
			}

			//convert the parsed xml-instances to basic instances
			LanesToLinkAssignment11 l2lAssignment;
			LaneData11 lane = null;
			for (XMLLanesToLinkAssignmentType lldef : xmlLaneDefinitions
					.getLanesToLinkAssignment()) {
				l2lAssignment = builder.createLanesToLinkAssignment(Id.create(lldef
						.getLinkIdRef(), Link.class));
				for (XMLLaneType laneType : lldef.getLane()) {
					lane = builder.createLane(Id.create(laneType.getId(), Lane.class));
					for (XMLIdRefType toLinkId : laneType.getToLink()) {
						lane.addToLinkId(Id.create(toLinkId.getRefId(), Link.class));
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
					lane.setStartsAtMeterFromLinkEnd(laneType.getLength().getMeter());
					l2lAssignment.addLane(lane);
				}
				this.laneDefinitions.addLanesToLinkAssignment(l2lAssignment);
			}
		} catch (JAXBException | SAXException | IOException e1) {
			throw new RuntimeException(e1);
		}


	}

}
