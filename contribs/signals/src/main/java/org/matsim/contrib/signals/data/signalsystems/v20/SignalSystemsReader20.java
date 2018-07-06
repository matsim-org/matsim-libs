/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsReader20
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
package org.matsim.contrib.signals.data.signalsystems.v20;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.signalsystems20.XMLIdRefType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystems;
import org.matsim.jaxb.signalsystems20.XMLSignalType;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLLane;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLTurningMoveRestrictions;
import org.matsim.lanes.Lane;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalSystemsReader20 implements MatsimReader {

	private SignalSystemsData signalSystemsData;

	public SignalSystemsReader20(SignalSystemsData signalSystemData) {
		this.signalSystemsData = signalSystemData;
	}

	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		readStream(IOUtils.getInputStream(filename));
	}

	public void readStream(InputStream stream) {
		XMLSignalSystems xmlssdefs = getXmlSignalSystems(stream);

		//convert from Jaxb types to MATSim-API conform types
		SignalSystemsDataFactory builder = this.signalSystemsData.getFactory();
		for (XMLSignalSystemType xmlss : xmlssdefs.getSignalSystem()){
			SignalSystemData ssdata = builder.createSignalSystemData(Id.create(xmlss.getId(), SignalSystem.class));
			this.signalSystemsData.addSignalSystemData(ssdata);
			for (XMLSignalType xmlsignal : xmlss.getSignals().getSignal()){
				SignalData signal = builder.createSignalData(Id.create(xmlsignal.getId(), Signal.class));
				signal.setLinkId(Id.create(xmlsignal.getLinkIdRef(), Link.class));
				ssdata.addSignalData(signal);
				if (xmlsignal.getLane() != null){
					for (XMLLane xmllane : xmlsignal.getLane()){
						signal.addLaneId(Id.create(xmllane.getRefId(), Lane.class));
					}
				}
				if (xmlsignal.getTurningMoveRestrictions() != null){
					XMLTurningMoveRestrictions tmr = xmlsignal.getTurningMoveRestrictions();
					for (XMLIdRefType xmlid : tmr.getToLink()){
						signal.addTurningMoveRestriction(Id.create(xmlid.getRefId(), Link.class));
					}
				}
			}
		}
	}

	private XMLSignalSystems getXmlSignalSystems(InputStream stream) {
		try {
			JAXBContext jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystems20.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/signalSystems_v2.0.xsd")));
			return (XMLSignalSystems) u.unmarshal(stream);
		} catch (JAXBException | SAXException e) {
			throw new UncheckedIOException(e);
		}
	}
}
