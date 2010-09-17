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
package org.matsim.signalsystems.data.signalsystems.v20;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.signalsystems20.XMLIdRefType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystems;
import org.matsim.jaxb.signalsystems20.XMLSignalType;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLLane;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLTurningMoveRestrictions;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalSystemsReader20 extends MatsimJaxbXmlParser {
	
	private static final Logger log = Logger.getLogger(SignalSystemsReader20.class);

	private SignalSystemsData signalSystemsData;

	private SignalSystemsDataFactory builder;

	public SignalSystemsReader20(SignalSystemsData signalSystemData, String schemaLocation) {
		super(schemaLocation);
		this.signalSystemsData = signalSystemData;
		this.builder = this.signalSystemsData.getFactory();
	}

	@Override
	public void readFile(final String filename) throws JAXBException,
			SAXException, ParserConfigurationException, IOException {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalSystems xmlssdefs = null;
		jc = JAXBContext
				.newInstance(org.matsim.jaxb.signalsystems20.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		log.info("starting to validate " + filename);
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
		InputStream stream = null;
		try {
		  stream = IOUtils.getInputstream(filename);
		  xmlssdefs = (XMLSignalSystems) u.unmarshal(stream);
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}
		
		//convert from Jaxb types to MATSim-API conform types
		for (XMLSignalSystemType xmlss : xmlssdefs.getSignalSystem()){
			SignalSystemData ssdata = this.builder.createSignalSystemData(new IdImpl(xmlss.getId()));
			this.signalSystemsData.addSignalSystemData(ssdata);
			for (XMLSignalType xmlsignal : xmlss.getSignals().getSignal()){
				SignalData signal = this.builder.createSignalData(new IdImpl(xmlsignal.getId()));
				signal.setLinkId(new IdImpl(xmlsignal.getLinkIdRef()));
				ssdata.addSignalData(signal);
				if (xmlsignal.getLane() != null){
					for (XMLLane xmllane : xmlsignal.getLane()){
						signal.addLaneId(new IdImpl(xmllane.getRefId()));
					}
				}
				if (xmlsignal.getTurningMoveRestrictions() != null){
					XMLTurningMoveRestrictions tmr = xmlsignal.getTurningMoveRestrictions();
					for (XMLIdRefType xmlid : tmr.getToLink()){
						signal.addTurningMoveRestriction(new IdImpl(xmlid.getRefId()));
					}
				}
			}
		}
		
	}

}
