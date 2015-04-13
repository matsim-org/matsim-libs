/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsReader20
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.signalgroups.v20;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.signalgroups20.XMLIdRefType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroupType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroups;
import org.matsim.jaxb.signalgroups20.XMLSignalSystemSignalGroupType;
import org.matsim.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalSystem;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 * @author jbischoff
 */
public class SignalGroupsReader20 extends MatsimJaxbXmlParser {

	private SignalGroupsData signalGroupsData;
	private SignalGroupsDataFactory factory;
	private static final Logger log = Logger.getLogger(SignalGroupsReader20.class);

	public SignalGroupsReader20(SignalGroupsData signalGroupsData, String schemaLocation) {
		super(schemaLocation);
		this.signalGroupsData = signalGroupsData;
		this.factory = signalGroupsData.getFactory();

	}

	public  SignalGroupsReader20(SignalGroupsData signalGroupsData) {
		this(signalGroupsData, MatsimSignalSystemsReader.SIGNALGROUPS20);
	}

	@Override
	public void readFile(String filename) {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalGroups xmlsgdefs = null;
		InputStream stream = null;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalgroups20.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			log.info("starting to validate " + filename);
			super.validateFile(filename, u);
			log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			xmlsgdefs = (XMLSignalGroups) u.unmarshal(stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (JAXBException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UncheckedIOException(e);
		} catch (ParserConfigurationException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}

		for (XMLSignalSystemSignalGroupType xsssgt : xmlsgdefs.getSignalSystem()) {
			// SigSys
			for (XMLSignalGroupType xsgt : xsssgt.getSignalGroup()) {
				SignalGroupData sgd = factory.createSignalGroupData(Id.create(xsssgt.getRefId(), SignalSystem.class), Id.create(xsgt.getId(), SignalGroup.class));

				for (XMLIdRefType id : xsgt.getSignal()) {
					sgd.addSignalId(Id.create(id.getRefId(), Signal.class));
				}
				signalGroupsData.addSignalGroupData(sgd);
			}
		}
	}

}
