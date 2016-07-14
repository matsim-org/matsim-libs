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
import org.matsim.jaxb.lanedefinitions11.ObjectFactory;
import org.matsim.jaxb.lanedefinitions11.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions11.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions11.XMLLaneType;
import org.matsim.jaxb.lanedefinitions11.XMLLanesToLinkAssignmentType;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
import org.matsim.lanes.data.v20.Lane;
import org.xml.sax.SAXException;


/**
 * Reader for the http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd
 * file format.
 * @author dgrether
 *
 */
public class LaneDefinitionsReader11 extends MatsimJaxbXmlParser implements MatsimSomeReader {

	private static final Logger log = Logger
			.getLogger(LaneDefinitionsReader11.class);

	private LaneDefinitions11 laneDefinitions;

	private LaneDefinitionsFactory11 builder;
	/**
	 * @deprecated use constructor without schema location
	 */
	@Deprecated
	public LaneDefinitionsReader11(LaneDefinitions11 laneDefs, String schemaLocation) {
		super(schemaLocation);
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getFactory();
	}
	
	public LaneDefinitionsReader11(LaneDefinitions11 laneDefs) {
		super(LaneDefinitionsReader.SCHEMALOCATIONV11);
		this.laneDefinitions = laneDefs;
		builder = this.laneDefinitions.getFactory();
	}
	

	/**
	 * @see org.matsim.core.utils.io.MatsimJaxbXmlParser#readFile(java.lang.String)
	 */
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
					e.printStackTrace();
					throw new RuntimeException(e.getMessage());
				}
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
			} catch (JAXBException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1.getMessage());
			} 	catch (SAXException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1.getMessage());
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1.getMessage());
			}

		
	}

}
