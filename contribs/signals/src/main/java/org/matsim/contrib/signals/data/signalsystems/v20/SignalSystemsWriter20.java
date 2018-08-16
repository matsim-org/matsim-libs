/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsWriter20
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.signalsystems20.ObjectFactory;
import org.matsim.jaxb.signalsystems20.XMLIdRefType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystems;
import org.matsim.jaxb.signalsystems20.XMLSignalType;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLLane;
import org.matsim.lanes.Lane;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;


/**
 * @author dgrether
 *
 */
public class SignalSystemsWriter20 extends MatsimJaxbXmlWriter {
	
	private static final Logger log = Logger.getLogger(SignalSystemsWriter20.class);
	
	private SignalSystemsData signalData;
	
	
	public SignalSystemsWriter20(SignalSystemsData signalSystemsData){
		this.signalData = signalSystemsData;
	}
	
	private XMLSignalSystems convertDataToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLSignalSystems xmlContainer = fac.createXMLSignalSystems();
		
		for (SignalSystemData ssd : this.signalData.getSignalSystemData().values()){
			XMLSignalSystemType xmlsstype = fac.createXMLSignalSystemType();
			xmlContainer.getSignalSystem().add(xmlsstype);
			xmlsstype.setId(ssd.getId().toString());
			
			if (ssd.getSignalData() != null){
				xmlsstype.setSignals(fac.createXMLSignalSystemTypeXMLSignals());
				
				for (SignalData sd : ssd.getSignalData().values()){
					XMLSignalType xmlssd = fac.createXMLSignalType();
					xmlsstype.getSignals().getSignal().add(xmlssd);
					xmlssd.setId(sd.getId().toString());
					xmlssd.setLinkIdRef(sd.getLinkId().toString());
					if (sd.getLaneIds() != null){
						for (Id<Lane> id : sd.getLaneIds()){
							XMLLane xmllane = fac.createXMLSignalTypeXMLLane();
							xmlssd.getLane().add(xmllane);
							xmllane.setRefId(id.toString());
						}
					}
					if (sd.getTurningMoveRestrictions() != null){
						xmlssd.setTurningMoveRestrictions(fac.createXMLSignalTypeXMLTurningMoveRestrictions());
						for (Id<Link> id : sd.getTurningMoveRestrictions()){
							XMLIdRefType xmlid = fac.createXMLIdRefType();
							xmlssd.getTurningMoveRestrictions().getToLink().add(xmlid);
							xmlid.setRefId(id.toString());
						}
					}
				}
			}
		}
		return xmlContainer;
	}

	public void write(final String filename, XMLSignalSystems xmlSignals){
		log.info("writing file: " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystems20.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimSignalSystemsReader.SIGNALSYSTEMS20, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(xmlSignals, bufout);
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
	public void write(final String filename) {
		XMLSignalSystems xmlSignals = convertDataToXml();
		this.write(filename, xmlSignals);
	}
	
}
