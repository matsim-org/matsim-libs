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
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.signalsystems11.XMLIdRefType;
import org.matsim.jaxb.signalsystems11.XMLSignalGroupDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystemDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystems;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsFactory;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class SignalSystemsReader11 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(SignalSystemsReader11.class);

	private SignalSystems lightSignalSystems;

	private SignalSystemsFactory builder;

	public SignalSystemsReader11(SignalSystems lightSignalSystems, String schemaLocation) {
		super(schemaLocation);
		this.lightSignalSystems = lightSignalSystems;
		this.builder = this.lightSignalSystems.getFactory();
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
		InputStream stream = null;
		try {
			stream = new FileInputStream(filename);
			xmlLssDefinition = (XMLSignalSystems) u.unmarshal(stream);
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}

		SignalSystemDefinition lssdef;
		for (XMLSignalSystemDefinitionType xmllssDef : xmlLssDefinition
				.getSignalSystemDefinition()) {
			lssdef = builder.createSignalSystemDefinition(new IdImpl(xmllssDef
					.getId()));
			if (xmllssDef.getDefaultCycleTime() != null) {
				lssdef.setDefaultCycleTime(xmllssDef.getDefaultCycleTime()
						.getSeconds());
			}
			if (xmllssDef.getDefaultInterGreenTime() !=  null) {
				lssdef.setDefaultInterGreenTime(xmllssDef.getDefaultInterGreenTime()
						.getSeconds());
			}
			if (xmllssDef.getDefaultSynchronizationOffset() != null) {
				lssdef.setDefaultSynchronizationOffset(xmllssDef
						.getDefaultSynchronizationOffset().getSeconds());
			}
			lightSignalSystems.addSignalSystemDefinition(lssdef);
		}
		// parsing lightSignalGroupDefinitions
		SignalGroupDefinition lsgdef;
		for (XMLSignalGroupDefinitionType xmllsgdef : xmlLssDefinition
				.getSignalGroupDefinition()) {
			lsgdef = builder.createSignalGroupDefinition(new IdImpl(xmllsgdef
					.getLinkIdRef()), new IdImpl(xmllsgdef.getId()));
			lsgdef.setSignalSystemDefinitionId(new IdImpl(xmllsgdef
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
