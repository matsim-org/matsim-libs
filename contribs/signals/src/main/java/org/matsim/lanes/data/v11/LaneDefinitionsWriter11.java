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
package org.matsim.lanes.data.v11;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.lanedefinitions11.ObjectFactory;
import org.matsim.jaxb.lanedefinitions11.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions11.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions11.XMLLaneType;
import org.matsim.jaxb.lanedefinitions11.XMLLanesToLinkAssignmentType;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
/**
 * Writer for the http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd
 * file format.
 * @author dgrether
 *
 */
public class LaneDefinitionsWriter11 extends MatsimJaxbXmlWriter implements MatsimSomeWriter {

	private static final Logger log = Logger
			.getLogger(LaneDefinitionsWriter11.class);

	private LaneDefinitions11 laneDefinitions;

	private XMLLaneDefinitions xmlLaneDefinitions;

	/**
	 * Writer for the http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd
	 * file format.
	 * @param lanedefs
	 *
	 */
	public LaneDefinitionsWriter11(LaneDefinitions11 lanedefs) {
		log.info("Using LaneDefinitionWriter11...");
		this.laneDefinitions = lanedefs;
		this.xmlLaneDefinitions = convertBasicToXml();
	}

	/**
	 * @see org.matsim.core.utils.io.MatsimJaxbXmlWriter#write(java.lang.String)
	 */
	@Override
	public void write(String filename) {
		log.info("writing to file: " + filename);
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.lanedefinitions11.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(LaneDefinitionsReader.SCHEMALOCATIONV11, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.xmlLaneDefinitions, bufout);
			bufout.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private XMLLaneDefinitions convertBasicToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLLaneDefinitions xmllaneDefs = fac.createXMLLaneDefinitions();

		for (LanesToLinkAssignment11 ltla : this.laneDefinitions.getLanesToLinkAssignments().values()) {
			XMLLanesToLinkAssignmentType xmlltla = fac.createXMLLanesToLinkAssignmentType();
			xmlltla.setLinkIdRef(ltla.getLinkId().toString());

			for (LaneData11 bl : ltla.getLanes().values()) {
				XMLLaneType xmllane = fac.createXMLLaneType();
				xmllane.setId(bl.getId().toString());

				for (Id<Link> id : bl.getToLinkIds()) {
					XMLIdRefType xmlToLink = fac.createXMLIdRefType();
					xmlToLink.setRefId(id.toString());
					xmllane.getToLink().add(xmlToLink);
				}

				XMLLaneType.XMLRepresentedLanes lanes = new XMLLaneType.XMLRepresentedLanes();
				lanes.setNumber(bl.getNumberOfRepresentedLanes());
				xmllane.setRepresentedLanes(lanes);

				XMLLaneType.XMLLength length = new XMLLaneType.XMLLength();
				length.setMeter(Double.valueOf(bl.getStartsAtMeterFromLinkEnd()));
				xmllane.setLength(length);

				xmlltla.getLane().add(xmllane);
			}
			xmllaneDefs.getLanesToLinkAssignment().add(xmlltla);
		} //end writing lanesToLinkAssignments

		return xmllaneDefs;
	}

}
