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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.signalcontrol20.XMLSignalControl;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemControllerType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemType;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalControlReader20 implements MatsimReader {

	private SignalControlData signalControlData;

	public SignalControlReader20(SignalControlData signalControlData){
		this.signalControlData = signalControlData;
	}

	private XMLSignalControl readSignalControl20Stream(InputStream stream) {
		try {
			JAXBContext jc = JAXBContext.newInstance(org.matsim.jaxb.signalcontrol20.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/signalControl_v2.0.xsd")));
			return (XMLSignalControl) u.unmarshal(stream);
		} catch (JAXBException | SAXException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void readFile(String filename) {
		readStream(IOUtils.getInputStream(filename));
	}

	private double getSeconds(XMLGregorianCalendar daytime) {
		double sec = daytime.getHour() * 3600.0;
		sec += daytime.getMinute() * 60.0;
		sec += daytime.getSecond();
		return sec;
	}

	public void readStream(InputStream inputStream) {
		XMLSignalControl xmlSignalControl = this.readSignalControl20Stream(inputStream);
		SignalControlDataFactory factory = this.signalControlData.getFactory();
		for (XMLSignalSystemType xmlSystem : xmlSignalControl.getSignalSystem()){
			SignalSystemControllerData controllerData = factory.createSignalSystemControllerData(Id.create(xmlSystem.getRefId(), SignalSystem.class));
			this.signalControlData.addSignalSystemControllerData(controllerData);
			XMLSignalSystemControllerType xmlController = xmlSystem.getSignalSystemController();
			controllerData.setControllerIdentifier(xmlController.getControllerIdentifier());
			for (XMLSignalPlanType xmlPlan : xmlController.getSignalPlan()){
				SignalPlanData plan = factory.createSignalPlanData(Id.create(xmlPlan.getId(), SignalPlan.class));
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
					SignalGroupSettingsData settings = factory.createSignalGroupSettingsData(Id.create(xmlSettings.getRefId(), SignalGroup.class));
					plan.addSignalGroupSettings(settings);
					settings.setDropping(xmlSettings.getDropping().getSec());
					settings.setOnset(xmlSettings.getOnset().getSec());
				}
			}
		}
	}

}
