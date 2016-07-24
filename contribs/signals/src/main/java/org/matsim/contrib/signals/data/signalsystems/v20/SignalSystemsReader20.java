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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.signalsystems20.XMLIdRefType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystems;
import org.matsim.jaxb.signalsystems20.XMLSignalType;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLLane;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLTurningMoveRestrictions;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalSystemsReader20 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(SignalSystemsReader20.class);

	private SignalSystemsData signalSystemsData;


	public SignalSystemsReader20(SignalSystemsData signalSystemData, String schemaLocation) {
		super(schemaLocation);
		this.signalSystemsData = signalSystemData;
	}

	public SignalSystemsReader20(SignalSystemsData signalSystemData) {
		this(signalSystemData, MatsimSignalSystemsReader.SIGNALSYSTEMS20);
	}

	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalSystems xmlssdefs = null;
		InputStream stream = null;
		try {
			jc = JAXBContext
					.newInstance(org.matsim.jaxb.signalsystems20.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			log.info("starting to validate " + filename);
			super.validateFile(filename, u);
			log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			xmlssdefs = (XMLSignalSystems) u.unmarshal(stream);
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
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}

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

}
