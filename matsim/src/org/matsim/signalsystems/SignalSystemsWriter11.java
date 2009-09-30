/* *********************************************************************** *
 * project: org.matsim.*
 * LightSignalSystemsWriter11
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
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.signalsystems11.ObjectFactory;
import org.matsim.jaxb.signalsystems11.XMLIdRefType;
import org.matsim.jaxb.signalsystems11.XMLMatsimTimeAttributeType;
import org.matsim.jaxb.signalsystems11.XMLSignalGroupDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystemDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.basic.BasicSignalSystemDefinition;
import org.matsim.signalsystems.basic.BasicSignalSystems;


/**
 * @author dgrether
 *
 */
public class SignalSystemsWriter11 extends MatsimJaxbXmlWriter {

	private static final Logger log = Logger
			.getLogger(SignalSystemsWriter11.class);
	
	private BasicSignalSystems blss;

	private XMLSignalSystems xmlLightSignalSystems;


	public SignalSystemsWriter11(BasicSignalSystems basiclss) {
		this.blss = basiclss;
		this.xmlLightSignalSystems = convertBasicToXml();
	}	
	
	@Override
	public void writeFile(final String filename) {
		log.info("writing file: " + filename);
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystems11.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimSignalSystemsReader.SIGNALSYSTEMS11, m);
			m.marshal(this.xmlLightSignalSystems, IOUtils.getBufferedWriter(filename)); 
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private XMLSignalSystems convertBasicToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLSignalSystems xmllss = fac.createXMLSignalSystems();
		
		//writing lightSignalSystemDefinitions
		for (BasicSignalSystemDefinition lssd : this.blss.getSignalSystemDefinitionsList()) {
			XMLSignalSystemDefinitionType xmllssd = fac.createXMLSignalSystemDefinitionType();
			xmllssd.setId(lssd.getId().toString());
			
			if (lssd.getDefaultCycleTime() != null) {
				XMLMatsimTimeAttributeType xmlcirculationtime = fac.createXMLMatsimTimeAttributeType();
				xmlcirculationtime.setSeconds(lssd.getDefaultCycleTime());
				xmllssd.setDefaultCycleTime(xmlcirculationtime);
			}

			if (lssd.getDefaultSynchronizationOffset() != null) {
				XMLMatsimTimeAttributeType xmlsyncoffset = fac.createXMLMatsimTimeAttributeType();
				xmlsyncoffset.setSeconds(lssd.getDefaultSynchronizationOffset());
				xmllssd.setDefaultSynchronizationOffset(xmlsyncoffset);
			}

			if (lssd.getDefaultInterGreenTime() != null) {
				XMLMatsimTimeAttributeType xmlinterimtime= fac.createXMLMatsimTimeAttributeType();
				xmlinterimtime.setSeconds(lssd.getDefaultInterGreenTime());
				xmllssd.setDefaultInterGreenTime(xmlinterimtime);
			}
			
			xmllss.getSignalSystemDefinition().add(xmllssd);
		}
		
		//writing lightSignalGroupDefinitions
		for (BasicSignalGroupDefinition lsgd : this.blss.getSignalGroupDefinitionsList()) {
			XMLSignalGroupDefinitionType xmllsgd = fac.createXMLSignalGroupDefinitionType();
			xmllsgd.setLinkIdRef(lsgd.getLinkRefId().toString());
			xmllsgd.setId(lsgd.getId().toString());
			
			XMLIdRefType lssdef = fac.createXMLIdRefType();
			if (lsgd.getSignalSystemDefinitionId() != null){
				lssdef.setRefId(lsgd.getSignalSystemDefinitionId().toString());
				xmllsgd.setSignalSystemDefinition(lssdef);
			}
			else {
				throw new IllegalStateException("Each signalGroupDefinitionType must have a signal system definition id assigned.");
			}
			
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
			
			xmllss.getSignalGroupDefinition().add(xmllsgd);
		}
		return xmllss;
	}
}
