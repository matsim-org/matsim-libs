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
package org.matsim.signalsystems.data.signalgroups.v20;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.signalgroups20.XMLIdRefType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroupType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroups;
import org.matsim.jaxb.signalgroups20.XMLSignalSystemSignalGroupType;
import org.matsim.signalsystems.MatsimSignalSystemsReader;

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
			stream = IOUtils.getInputstream(filename);
			xmlsgdefs = (XMLSignalGroups) u.unmarshal(stream);
		} catch (Exception e) {
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
				SignalGroupData sgd = factory.createSignalGroupData(new IdImpl(xsssgt.getRefId()), new IdImpl(xsgt.getId()));

				for (XMLIdRefType id : xsgt.getSignal()) {
					sgd.addSignalId(new IdImpl(id.getRefId()));
				}
				signalGroupsData.addSignalGroupData(sgd);
			}
		}
	}

}
