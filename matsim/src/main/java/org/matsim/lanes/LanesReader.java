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
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.jaxb.lanedefinitions20.ObjectFactory;
import org.matsim.jaxb.lanedefinitions20.XMLAttributeType;
import org.matsim.jaxb.lanedefinitions20.XMLIdRefType;
import org.matsim.jaxb.lanedefinitions20.XMLLaneDefinitions;
import org.matsim.jaxb.lanedefinitions20.XMLLaneType;
import org.matsim.jaxb.lanedefinitions20.XMLLanesToLinkAssignmentType;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public final class LanesReader implements MatsimReader {
	
	private static final Logger log = LogManager.getLogger(LanesReader.class);
	
	@Deprecated
	public static final String SCHEMALOCATIONV11 = "http://www.matsim.org/files/dtd/laneDefinitions_v1.1.xsd";

	public static final String SCHEMALOCATIONV20 = "http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd";
	
	private Lanes lanes;
	private LanesFactory factory;

	private final ObjectAttributesConverter attributesConverter = new ObjectAttributesConverter();

	public LanesReader(Scenario scenario) {
		this.lanes = scenario.getLanes();
		this.factory = lanes.getFactory();
	}

	@Override
	public void readFile(final String filename) {
		try {
			log.info("reading file " + filename);
			InputStream inputStream = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
			parse(inputStream);
		} catch (JAXBException | SAXException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void readURL( final URL url ) {
		try {
			log.info("reading file " + url.toString());
			InputStream inputStream = IOUtils.getInputStream(url);
			parse(inputStream);
		} catch (JAXBException | SAXException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void parse(InputStream stream) throws JAXBException, SAXException {
		ObjectFactory fac = new ObjectFactory();
		JAXBContext jc = JAXBContext.newInstance(org.matsim.jaxb.lanedefinitions20.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/laneDefinitions_v2.0.xsd")));

		XMLLaneDefinitions xmlLaneDefinitions;
		try {
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
		for (XMLLanesToLinkAssignmentType lldef : xmlLaneDefinitions
				.getLanesToLinkAssignment()) {
			LanesToLinkAssignment l2lAssignment = factory.createLanesToLinkAssignment(Id.create(lldef
					.getLinkIdRef(), Link.class));
			for (XMLLaneType laneType : lldef.getLane()) {
				Lane lane = factory.createLane(Id.create(laneType.getId(), Lane.class));

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

				if (laneType.getAttributes()!=null && !laneType.getAttributes().getAttributeList().isEmpty()) {
					for (XMLAttributeType att : laneType.getAttributes().getAttributeList()){
						Object attribute = attributesConverter.convert(att.getClazz(), att.getValue());
						// Note: when I refactored this, the behavior was that if a converter was not found,
						// the attribute was read as String. This is inconsistent with the way attributes are read normally,
						// and I cannot see a use for it, so I just ignored the attribute, as is done in other readers.
						// td, apr 18
						if (attribute != null) lane.getAttributes().putAttribute(att.getName(), attribute);
					}
				}

				l2lAssignment.addLane(lane);
			}
			this.lanes.addLanesToLinkAssignment(l2lAssignment);
		}
	}
}
