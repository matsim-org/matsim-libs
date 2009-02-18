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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.signalsystems.BasicSignalSystemsFactory;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.xml.lightsignalsystems.ObjectFactory;
import org.matsim.basic.xml.lightsignalsystems.XMLIdRefType;
import org.matsim.basic.xml.lightsignalsystems.XMLLaneType;
import org.matsim.basic.xml.lightsignalsystems.XMLLanesToLinkAssignmentType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalGroupDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystemDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystems;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class MatsimLightSignalSystemsReader {
	
	private BasicSignalSystems lightSignalSystems;
   
  private BasicSignalSystemsFactory factory = new BasicSignalSystemsFactory();

	
	public MatsimLightSignalSystemsReader(BasicSignalSystems lightSignalSystems) {
		this.lightSignalSystems = lightSignalSystems;
	}
	
	
	public void readFile(final String filename) {
  	JAXBContext jc;
    XMLLightSignalSystems xmlLssDefinition;
		try {
			
			jc = JAXBContext.newInstance(org.matsim.basic.xml.lightsignalsystems.ObjectFactory.class);
			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = parserFactory.newSAXParser();
//			XMLReader xmlReader = saxParser.getXMLReader();
//			MatsimXmlEntityResolver resolver = new MatsimXmlEntityResolver();
//			EntityResolver resolver = new MatsimXmlParser();
//			xmlReader.setEntityResolver(resolver);
//			xmlReader.setContentHandler(resolver);
			
//			SAXSource saxSource = new SAXSource(xmlReader, new InputSource(new FileInputStream(filename)));
			
			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
//			schemaFac.
			Schema schema = schemaFac.newSchema(new URL("http://www.matsim.org/files/dtd/lightSignalSystems_v1.0.xsd"));
			u.setSchema(schema);
//			xmlLssDefinition = (XMLLightSignalSystems)u.unmarshal(saxSource);
			xmlLssDefinition = (XMLLightSignalSystems)u.unmarshal(new FileInputStream(filename));

			BasicLanesToLinkAssignment l2lAssignment;
			BasicLane lane = null;
			for (XMLLanesToLinkAssignmentType lldef : xmlLssDefinition.getLanesToLinkAssignment()) {
				l2lAssignment = factory.createLanesToLinkAssignment(new IdImpl(lldef.getLinkIdRef()));
				for (XMLLaneType laneType : lldef.getLane()) {
					lane = factory.createLane(new IdImpl(laneType.getId()));
					for (XMLIdRefType toLinkId : laneType.getToLink()) {
						lane.addToLinkId(new IdImpl(toLinkId.getRefId()));
					}
					if (laneType.getRepresentedLanes() == null) {
						laneType.setRepresentedLanes(fac.createXMLLaneTypeXMLRepresentedLanes());
					}
					lane.setNumberOfRepresentedLanes(laneType.getRepresentedLanes().getNumber());
					if (laneType.getLength() == null) {
						laneType.setLength(fac.createXMLLaneTypeXMLLength());
					}
					lane.setLength(laneType.getLength().getMeter());
					l2lAssignment.addLane(lane);
				}
				lightSignalSystems.addLanesToLinkAssignment(l2lAssignment);
			}
			
			BasicSignalSystemDefinition lssdef;
			for (XMLLightSignalSystemDefinitionType xmllssDef : xmlLssDefinition.getLightSignalSystemDefinition()) {
				lssdef = factory.createLightSignalSystemDefinition(new IdImpl(xmllssDef.getId()));
				lssdef.setDefaultCirculationTime(xmllssDef.getDefaultCirculationTime().getSeconds());
				lssdef.setDefaultInterimTime(xmllssDef.getDefaultInterimTime().getSeconds());
				lssdef.setDefaultSyncronizationOffset(xmllssDef.getDefaultSyncronizationOffset().getSeconds());
				lightSignalSystems.addSignalSystemDefinition(lssdef);
			}
			//parsing lightSignalGroupDefinitions
			BasicSignalGroupDefinition lsgdef;
			for (XMLLightSignalGroupDefinitionType xmllsgdef : xmlLssDefinition.getLightSignalGroupDefinition()) {
				lsgdef = factory.createLightSignalGroupDefinition(new IdImpl(xmllsgdef.getLinkIdRef()), new IdImpl(xmllsgdef.getId()));
				lsgdef.setLightSignalSystemDefinitionId(new IdImpl(xmllsgdef.getLightSignalSystemDefinition().getRefId()));
				for (XMLIdRefType refIds : xmllsgdef.getLane()) {
					lsgdef.addLaneId(new IdImpl(refIds.getRefId()));
				}
				for (XMLIdRefType refIds : xmllsgdef.getToLink()) {
					lsgdef.addToLinkId(new IdImpl(refIds.getRefId()));
				}
				lightSignalSystems.addSignalGroupDefinition(lsgdef);
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (MalformedURLException e) {
			 e.printStackTrace();
		 }
//		catch (SAXException e) {
//			 e.printStackTrace();
//		} 
 catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

	}
}
