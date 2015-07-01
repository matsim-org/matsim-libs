/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimLaneWriter
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.MarshalException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.lanedefinitions20.ObjectFactory;
import org.matsim.jaxb.lanedefinitions20.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions20.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions20.XMLLaneType;
import org.matsim.jaxb.lanedefinitions20.XMLLanesToLinkAssignmentType;
import org.matsim.lanes.data.MatsimLaneDefinitionsReader;
/**
 * Writer for the http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd
 * file format.
 * @author dgrether
 *
 */
public class LaneDefinitionsWriter20 extends MatsimJaxbXmlWriter implements MatsimSomeWriter {

	private static final Logger log = Logger
			.getLogger(LaneDefinitionsWriter20.class);

	private LaneDefinitions20 laneDefinitions;

	private XMLLaneDefinitions xmlLaneDefinitions;

	/**
	 * Writer for the http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd
	 * file format.
	 * @param lanedefs
	 *
	 */
	public LaneDefinitionsWriter20(LaneDefinitions20 lanedefs) {
		log.info("Using LaneDefinitionWriter20...");
		this.laneDefinitions = lanedefs;
	}

	/**
	 * @see org.matsim.core.utils.io.MatsimJaxbXmlWriter#write(java.lang.String)
	 */
	@Override
	public void write(String filename) {
		log.info("writing to file: " + filename);
  	JAXBContext jc;
		try {
			this.xmlLaneDefinitions = convertDataToXml();
			jc = JAXBContext.newInstance(org.matsim.jaxb.lanedefinitions20.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimLaneDefinitionsReader.SCHEMALOCATIONV20, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.xmlLaneDefinitions, bufout);
			bufout.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MarshalException e) {
			e.printStackTrace();
		}
	}

	private XMLLaneDefinitions convertDataToXml() throws MarshalException {
		ObjectFactory fac = new ObjectFactory();
		XMLLaneDefinitions xmllaneDefs = fac.createXMLLaneDefinitions();

		for (LanesToLinkAssignment20 ltla : this.laneDefinitions.getLanesToLinkAssignments().values()) {
			XMLLanesToLinkAssignmentType xmlltla = fac.createXMLLanesToLinkAssignmentType();
			xmlltla.setLinkIdRef(ltla.getLinkId().toString());

			for (Lane bl : ltla.getLanes().values()) {
				XMLLaneType xmllane = fac.createXMLLaneType();
				xmllane.setId(bl.getId().toString());

				if ((bl.getToLinkIds() == null && bl.getToLaneIds() != null) || 
						(bl.getToLinkIds() != null && bl.getToLaneIds() == null)){
					xmllane.setLeadsTo(fac.createXMLLaneTypeXMLLeadsTo());
				}
				else {
					throw new MarshalException("Either at least one toLinkId or (exclusive) one toLaneId must" +
							"be set for Lane Id " + bl.getId() + " on link Id " + ltla.getLinkId() + "! Cannot write according to XML grammar.");
				}
				
				if (bl.getToLinkIds() != null){
					for (Id<Link> id : bl.getToLinkIds()) {
						XMLIdRefType xmlToLink = fac.createXMLIdRefType();
						xmlToLink.setRefId(id.toString());
						xmllane.getLeadsTo().getToLink().add(xmlToLink);
					}
				}
				else if (bl.getToLaneIds() != null){
					for (Id<Lane> id : bl.getToLaneIds()) {
						XMLIdRefType xmlToLink = fac.createXMLIdRefType();
						xmlToLink.setRefId(id.toString());
						xmllane.getLeadsTo().getToLane().add(xmlToLink);
					}
				}
				
				XMLLaneType.XMLCapacity capacity = new XMLLaneType.XMLCapacity();
				capacity.setVehiclesPerHour(bl.getCapacityVehiclesPerHour());
				xmllane.setCapacity(capacity);

				XMLLaneType.XMLRepresentedLanes lanes = new XMLLaneType.XMLRepresentedLanes();
				lanes.setNumber(Double.valueOf(bl.getNumberOfRepresentedLanes()));
				xmllane.setRepresentedLanes(lanes);

				XMLLaneType.XMLStartsAt startsAt = new XMLLaneType.XMLStartsAt();
				startsAt.setMeterFromLinkEnd(Double.valueOf(bl.getStartsAtMeterFromLinkEnd()));
				xmllane.setStartsAt(startsAt);

				xmllane.setAlignment(bl.getAlignment());
				
				xmlltla.getLane().add(xmllane);
			}
			xmllaneDefs.getLanesToLinkAssignment().add(xmlltla);
		} //end writing lanesToLinkAssignments

		return xmllaneDefs;
	}

}
