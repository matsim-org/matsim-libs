/* *********************************************************************** *
 * project: org.matsim.*
 * LightSignalSystemsReader11
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

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.signalsystems.BasicSignalSystemsBuilder;
import org.matsim.basic.v01.IdImpl;
import org.matsim.jaxb.signalsystems11.XMLIdRefType;
import org.matsim.jaxb.signalsystems11.XMLSignalGroupDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystemDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystems;
import org.matsim.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 * 
 */
public class SignalSystemsReader11 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger
			.getLogger(SignalSystemsReader11.class);

	private BasicSignalSystems lightSignalSystems;

	private BasicSignalSystemsBuilder builder;

	SignalSystemsReader11(BasicSignalSystems lightSignalSystems, String schemaLocation) {
		super(schemaLocation);
		this.lightSignalSystems = lightSignalSystems;
		this.builder = this.lightSignalSystems.getSignalSystemsBuilder();
	}

	@Override
	public void readFile(final String filename) throws JAXBException,
			SAXException, ParserConfigurationException, IOException {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalSystems xmlLssDefinition;
		jc = JAXBContext
				.newInstance(org.matsim.jaxb.signalsystems11.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
		xmlLssDefinition = (XMLSignalSystems) u.unmarshal(new FileInputStream(
				filename));

		BasicSignalSystemDefinition lssdef;
		for (XMLSignalSystemDefinitionType xmllssDef : xmlLssDefinition
				.getSignalSystemDefinition()) {
			lssdef = builder.createLightSignalSystemDefinition(new IdImpl(xmllssDef
					.getId()));
			lssdef.setDefaultCirculationTime(xmllssDef.getDefaultCirculationTime()
					.getSeconds());
			lssdef.setDefaultInterimTime(xmllssDef.getDefaultInterimTime()
					.getSeconds());
			lssdef.setDefaultSyncronizationOffset(xmllssDef
					.getDefaultSyncronizationOffset().getSeconds());
			lightSignalSystems.addSignalSystemDefinition(lssdef);
		}
		// parsing lightSignalGroupDefinitions
		BasicSignalGroupDefinition lsgdef;
		for (XMLSignalGroupDefinitionType xmllsgdef : xmlLssDefinition
				.getSignalGroupDefinition()) {
			lsgdef = builder.createLightSignalGroupDefinition(new IdImpl(xmllsgdef
					.getLinkIdRef()), new IdImpl(xmllsgdef.getId()));
			lsgdef.setLightSignalSystemDefinitionId(new IdImpl(xmllsgdef
					.getSignalSystemDefinition().getRefId()));
			for (XMLIdRefType refIds : xmllsgdef.getLane()) {
				lsgdef.addLaneId(new IdImpl(refIds.getRefId()));
			}
			for (XMLIdRefType refIds : xmllsgdef.getToLink()) {
				lsgdef.addToLinkId(new IdImpl(refIds.getRefId()));
			}
			lightSignalSystems.addSignalGroupDefinition(lsgdef);
		}

	}
}
