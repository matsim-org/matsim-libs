/* *********************************************************************** *
 * project: org.matsim.*
 * LightSignalSystems10Writer
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
package org.matsim.signalsystems;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.lightsignalsystems10.ObjectFactory;
import org.matsim.jaxb.lightsignalsystems10.XMLIdRefType;
import org.matsim.jaxb.lightsignalsystems10.XMLLaneType;
import org.matsim.jaxb.lightsignalsystems10.XMLLanesToLinkAssignmentType;
import org.matsim.jaxb.lightsignalsystems10.XMLLightSignalGroupDefinitionType;
import org.matsim.jaxb.lightsignalsystems10.XMLLightSignalSystemDefinitionType;
import org.matsim.jaxb.lightsignalsystems10.XMLLightSignalSystems;
import org.matsim.jaxb.lightsignalsystems10.XMLMatsimTimeAttributeType;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;


/**
 * Writer for the lightSignalSystems_v1.0.xsd file format.
 * @author dgrether
 * @deprecated use 1.1 writer 
 */
@Deprecated
public class LightSignalSystemsWriter10 extends MatsimJaxbXmlWriter {

	private static final Logger log = Logger
			.getLogger(LightSignalSystemsWriter10.class);
	
	private SignalSystems blss;

	private XMLLightSignalSystems xmlLightSignalSystems;

	private LaneDefinitions lanes;

	public LightSignalSystemsWriter10(LaneDefinitions lanes, SignalSystems basiclss) {
		this.lanes = lanes;
		this.blss = basiclss;
		this.xmlLightSignalSystems = convertBasicToXml();
	}	
	
	@Override
	public void writeFile(final String filename) {
		log.info("writing file: " + filename);
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.lightsignalsystems10.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimSignalSystemsReader.SIGNALSYSTEMS10, m);
			m.marshal(this.xmlLightSignalSystems, IOUtils.getBufferedWriter(filename)); 
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private XMLLightSignalSystems convertBasicToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLLightSignalSystems xmllss = fac.createXMLLightSignalSystems();
		
		for (LanesToLinkAssignment ltla : this.lanes.getLanesToLinkAssignmentsList()) {
			XMLLanesToLinkAssignmentType xmlltla = fac.createXMLLanesToLinkAssignmentType();
			xmlltla.setLinkIdRef(ltla.getLinkId().toString());
			
			for (Lane bl : ltla.getLanesList()) {
				XMLLaneType xmllane = fac.createXMLLaneType();
				xmllane.setId(bl.getId().toString());
				
				for (Id id : bl.getToLinkIds()) {
					XMLIdRefType xmlToLink = fac.createXMLIdRefType();
					xmlToLink.setRefId(id.toString());
					xmllane.getToLink().add(xmlToLink);
				}

				XMLLaneType.XMLRepresentedLanes lanes = new XMLLaneType.XMLRepresentedLanes();
				lanes.setNumber(Integer.valueOf(bl.getNumberOfRepresentedLanes()));
				xmllane.setRepresentedLanes(lanes);
				
				XMLLaneType.XMLLength length = new XMLLaneType.XMLLength();
				length.setMeter(Double.valueOf(bl.getLength()));
				xmllane.setLength(length);
				
				xmlltla.getLane().add(xmllane);
			}
			xmllss.getLanesToLinkAssignment().add(xmlltla);
		} //end writing lanesToLinkAssignments
		
		//writing lightSignalSystemDefinitions
		for (SignalSystemDefinition lssd : this.blss.getSignalSystemDefinitionsList()) {
			XMLLightSignalSystemDefinitionType xmllssd = fac.createXMLLightSignalSystemDefinitionType();
			xmllssd.setId(lssd.getId().toString());
			
			XMLMatsimTimeAttributeType xmlcirculationtime = fac.createXMLMatsimTimeAttributeType();
			xmlcirculationtime.setSeconds(lssd.getDefaultCycleTime());
			xmllssd.setDefaultCirculationTime(xmlcirculationtime);
			
			XMLMatsimTimeAttributeType xmlsyncoffset = fac.createXMLMatsimTimeAttributeType();
			xmlsyncoffset.setSeconds(lssd.getDefaultSynchronizationOffset());
			xmllssd.setDefaultSyncronizationOffset(xmlsyncoffset);
			
			XMLMatsimTimeAttributeType xmlinterimtime= fac.createXMLMatsimTimeAttributeType();
			xmlinterimtime.setSeconds(lssd.getDefaultInterGreenTime());
			xmllssd.setDefaultInterimTime(xmlinterimtime);
			
			xmllss.getLightSignalSystemDefinition().add(xmllssd);
		}
		
		//writing lightSignalGroupDefinitions
		for (SignalGroupDefinition lsgd : this.blss.getSignalGroupDefinitionsList()) {
			XMLLightSignalGroupDefinitionType xmllsgd = fac.createXMLLightSignalGroupDefinitionType();
			xmllsgd.setLinkIdRef(lsgd.getLinkRefId().toString());
			xmllsgd.setId(lsgd.getId().toString());
			
			XMLIdRefType lssdef = fac.createXMLIdRefType();
			lssdef.setRefId(lsgd.getSignalSystemDefinitionId().toString());
			xmllsgd.setLightSignalSystemDefinition(lssdef);
			
			for (Id laneid : lsgd.getLaneIds()) {
				XMLIdRefType xmllaneid = fac.createXMLIdRefType();
				xmllaneid.setRefId(laneid.toString());
				xmllsgd.getLane().add(xmllaneid);
			}

			for (Id tolinkid : lsgd.getToLinkIds()) {
				XMLIdRefType xmltolinkid = fac.createXMLIdRefType();
				xmltolinkid.setRefId(tolinkid.toString());
				xmllsgd.getToLink().add(xmltolinkid);
			}
			
			xmllss.getLightSignalGroupDefinition().add(xmllsgd);
		}
		return xmllss;
	}
}
