/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsWriter20
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.signalgroups20.ObjectFactory;
import org.matsim.jaxb.signalgroups20.XMLIdRefType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroupType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroups;
import org.matsim.jaxb.signalgroups20.XMLSignalSystemSignalGroupType;
import org.matsim.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalSystem;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;

/**
 * @author dgrether
 * @author jbischoff
 * 
 */
public class SignalGroupsWriter20 extends MatsimJaxbXmlWriter {

	private static final Logger log = Logger.getLogger(SignalGroupsWriter20.class);

	private SignalGroupsData signalGroupsData;

	public SignalGroupsWriter20(SignalGroupsData signalGroupsData) {
		this.signalGroupsData = signalGroupsData;
	}

	public void write(final String filename, XMLSignalGroups xmlSignalGroups) {
		log.info("writing file: " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalgroups20.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimSignalSystemsReader.SIGNALGROUPS20, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(xmlSignalGroups, bufout);
			bufout.close();
			log.info(filename + " written successfully.");
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void write(String filename) {
		XMLSignalGroups xmlSignalGroups = this.convertDataToXml();
		this.write(filename, xmlSignalGroups);
	}

	private XMLSignalGroups convertDataToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLSignalGroups xmlContainer = fac.createXMLSignalGroups();

		for (Entry<Id<SignalSystem>, Map<Id<SignalGroup>, SignalGroupData>> ssid : this.signalGroupsData
				.getSignalGroupDataBySignalSystemId().entrySet()) {
			// signal System
			XMLSignalSystemSignalGroupType xsssgt = fac.createXMLSignalSystemSignalGroupType();
			xsssgt.setRefId(ssid.getKey().toString());

			for (Entry<Id<SignalGroup>, SignalGroupData> signal : ssid.getValue().entrySet()) {
				XMLSignalGroupType xsgt = fac.createXMLSignalGroupType();
				xsgt.setId(signal.getValue().getId().toString());
				for (Id<Signal> ids : signal.getValue().getSignalIds()) {
					XMLIdRefType xmlid = fac.createXMLIdRefType();
					xmlid.setRefId(ids.toString());
					xsgt.getSignal().add(xmlid);
				}
				xsssgt.getSignalGroup().add(xsgt);

			}
			xmlContainer.getSignalSystem().add(xsssgt);
		}
		return xmlContainer;
	}
}
