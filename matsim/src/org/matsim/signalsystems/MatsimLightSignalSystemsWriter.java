/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.v01.Id;
import org.matsim.basic.xml.lightsignalsystems.ObjectFactory;
import org.matsim.basic.xml.lightsignalsystems.XMLIdRefType;
import org.matsim.basic.xml.lightsignalsystems.XMLLaneType;
import org.matsim.basic.xml.lightsignalsystems.XMLLanesToLinkAssignmentType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalGroupDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystemDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystems;
import org.matsim.basic.xml.lightsignalsystems.XMLMatsimTimeAttributeType;
import org.matsim.utils.io.IOUtils;

/**
 * @author dgrether
 */
public class MatsimLightSignalSystemsWriter {
	
	private BasicSignalSystems blss;

	private XMLLightSignalSystems xmlLightSignalSystems;
	
	public MatsimLightSignalSystemsWriter(BasicSignalSystems basiclss) {
		this.blss = basiclss;
		this.xmlLightSignalSystems = convertBasicToXml();
	}	
	
	public void writeFile(final String filename) {
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.basic.xml.lightsignalsystems.ObjectFactory.class);
			Marshaller m = jc.createMarshaller(); 
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, 
					Boolean.TRUE); 
			
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
		
		for (BasicLanesToLinkAssignment ltla : this.blss.getLanesToLinkAssignments()) {
			XMLLanesToLinkAssignmentType xmlltla = fac.createXMLLanesToLinkAssignmentType();
			xmlltla.setLinkIdRef(ltla.getLinkId().toString());
			
			for (BasicLane bl : ltla.getLanes()) {
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
		for (BasicSignalSystemDefinition lssd : this.blss.getSignalSystemDefinitions()) {
			XMLLightSignalSystemDefinitionType xmllssd = fac.createXMLLightSignalSystemDefinitionType();
			xmllssd.setId(lssd.getId().toString());
			
			XMLMatsimTimeAttributeType xmlcirculationtime = fac.createXMLMatsimTimeAttributeType();
			xmlcirculationtime.setSeconds(lssd.getDefaultCirculationTime());
			xmllssd.setDefaultCirculationTime(xmlcirculationtime);
			
			XMLMatsimTimeAttributeType xmlsyncoffset = fac.createXMLMatsimTimeAttributeType();
			xmlsyncoffset.setSeconds(lssd.getDefaultSyncronizationOffset());
			xmllssd.setDefaultSyncronizationOffset(xmlsyncoffset);
			
			XMLMatsimTimeAttributeType xmlinterimtime= fac.createXMLMatsimTimeAttributeType();
			xmlinterimtime.setSeconds(lssd.getDefaultInterimTime());
			xmllssd.setDefaultInterimTime(xmlinterimtime);
			
			xmllss.getLightSignalSystemDefinition().add(xmllssd);
		}
		
		//writing lightSignalGroupDefinitions
		for (BasicSignalGroupDefinition lsgd : this.blss.getSignalGroupDefinitions()) {
			XMLLightSignalGroupDefinitionType xmllsgd = fac.createXMLLightSignalGroupDefinitionType();
			xmllsgd.setLinkIdRef(lsgd.getLinkRefId().toString());
			xmllsgd.setId(lsgd.getId().toString());
			
			XMLIdRefType lssdef = fac.createXMLIdRefType();
			lssdef.setRefId(lsgd.getLightSignalSystemDefinitionId().toString());
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
