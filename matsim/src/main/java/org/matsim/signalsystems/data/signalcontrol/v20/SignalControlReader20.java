/* *********************************************************************** *
 * project: org.matsim.*
 * SignalControlReader20
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
package org.matsim.signalsystems.data.signalcontrol.v20;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.signalcontrol20.XMLSignalControl;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemControllerType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemType;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalControlReader20 extends MatsimJaxbXmlParser {

	
	private static final Logger log = Logger.getLogger(SignalControlReader20.class);
	
	private SignalControlData signalControlData;
	
	
	public SignalControlReader20(SignalControlData signalControlData){
		super(MatsimSignalSystemsReader.SIGNALCONTROL20);
		this.signalControlData = signalControlData;
	}
	
	public SignalControlReader20(SignalControlData signalControlData, String schemaLocation) {
		super(schemaLocation);
		this.signalControlData = signalControlData;
	}

	public XMLSignalControl readSignalControl20File(String filename) throws JAXBException, SAXException, ParserConfigurationException, IOException{
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalControl xmlSignalControl;
		jc = JAXBContext
				.newInstance(org.matsim.jaxb.signalcontrol20.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
		InputStream stream = null;
		try {
//			stream = new FileInputStream(filename);
		  stream = IOUtils.getInputstream(filename);
		  xmlSignalControl = (XMLSignalControl) u.unmarshal(stream);
			log.info("unmarshalling complete");
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}
		return xmlSignalControl;
	}

	
	
	@Override
	public void readFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		SignalControlDataFactory factory = this.signalControlData.getFactory();

		XMLSignalControl xmlSignalControl = this.readSignalControl20File(filename);
		for (XMLSignalSystemType xmlSystem : xmlSignalControl.getSignalSystem()){
			SignalSystemControllerData controllerData = factory.createSignalSystemControllerData(new IdImpl(xmlSystem.getRefId()));
			this.signalControlData.addSignalSystemControllerData(controllerData);
			XMLSignalSystemControllerType xmlController = xmlSystem.getSignalSystemController();
			controllerData.setControllerIdentifier(xmlController.getControllerIdentifier());
			for (XMLSignalPlanType xmlPlan : xmlController.getSignalPlan()){
				SignalPlanData plan = factory.createSignalPlanData(new IdImpl(xmlPlan.getId()));
				controllerData.addSignalPlanData(plan);
				if (xmlPlan.getCycleTime() != null){
					plan.setCycleTime(xmlPlan.getCycleTime().getSec());
				}
				if (xmlPlan.getOffset() != null){
					plan.setOffset(xmlPlan.getOffset().getSec());
				}
				if (xmlPlan.getStart() != null){
					plan.setStartTime(this.getSeconds(xmlPlan.getStart().getDaytime()));
				}
				if (xmlPlan.getStop() != null){
					plan.setEndTime(this.getSeconds(xmlPlan.getStop().getDaytime()));
				}
				for (XMLSignalGroupSettingsType xmlSettings: xmlPlan.getSignalGroupSettings()){
					SignalGroupSettingsData settings = factory.createSignalGroupSettingsData(new IdImpl(xmlSettings.getRefId()));
					plan.addSignalGroupSettings(settings);
					settings.setDropping(xmlSettings.getDropping().getSec());
					settings.setOnset(xmlSettings.getOnset().getSec());
				}
			}
		}
	}

	private double getSeconds(XMLGregorianCalendar daytime) {
		double sec = daytime.getHour() * 3600.0;
		sec += daytime.getMinute() * 60.0;
		sec += daytime.getSecond();
		return sec;
	}

	
}
