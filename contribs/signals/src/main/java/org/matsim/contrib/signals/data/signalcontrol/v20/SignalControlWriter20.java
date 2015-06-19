/* *********************************************************************** *
 * project: org.matsim.*
 * SignalControlWriter20
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.signalcontrol20.ObjectFactory;
import org.matsim.jaxb.signalcontrol20.XMLSignalControl;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType.XMLDropping;
import org.matsim.jaxb.signalcontrol20.XMLSignalGroupSettingsType.XMLOnset;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLCycleTime;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLOffset;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLStart;
import org.matsim.jaxb.signalcontrol20.XMLSignalPlanType.XMLStop;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemControllerType;
import org.matsim.jaxb.signalcontrol20.XMLSignalSystemType;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;


/**
 * @author dgrether
 *
 */
public class SignalControlWriter20 extends MatsimJaxbXmlWriter {

	private static final Logger log = Logger.getLogger(SignalControlWriter20.class);
	
	private SignalControlData data;

	public SignalControlWriter20(SignalControlData controlData) {
		this.data = controlData;
	}

	private XMLSignalControl convertDataToXml() throws DatatypeConfigurationException{
		ObjectFactory fac = new ObjectFactory();
		XMLSignalControl xmlSignalControl = fac.createXMLSignalControl();
		for (SignalSystemControllerData cd : this.data.getSignalSystemControllerDataBySystemId().values()){
			XMLSignalSystemType xmlSS = fac.createXMLSignalSystemType();
			xmlSignalControl.getSignalSystem().add(xmlSS);
			xmlSS.setRefId(cd.getSignalSystemId().toString());
			
			XMLSignalSystemControllerType xmlCd = fac.createXMLSignalSystemControllerType();
			xmlSS.setSignalSystemController(xmlCd);
			xmlCd.setControllerIdentifier(cd.getControllerIdentifier());
			
			//process plans if there are any
			if (cd.getSignalPlanData() != null){
				for (SignalPlanData planData : cd.getSignalPlanData().values()){
					XMLSignalPlanType xmlPd = fac.createXMLSignalPlanType();
					xmlCd.getSignalPlan().add(xmlPd);
					xmlPd.setId(planData.getId().toString());
					if (planData.getStartTime() != null){
						XMLStart xmlStart = fac.createXMLSignalPlanTypeXMLStart();
						xmlPd.setStart(xmlStart);
						xmlStart.setDaytime(this.getXmlGregorianCalendar(planData.getStartTime()));
					}
					if (planData.getEndTime() != null){
						XMLStop xmlStop = fac.createXMLSignalPlanTypeXMLStop();
						xmlPd.setStop(xmlStop);
						xmlStop.setDaytime(this.getXmlGregorianCalendar(planData.getEndTime()));
					}
					if (planData.getCycleTime() != null){
						XMLCycleTime xmlCycleTime = fac.createXMLSignalPlanTypeXMLCycleTime();
						xmlPd.setCycleTime(xmlCycleTime);
						xmlCycleTime.setSec(planData.getCycleTime());
					}
					if (planData.getOffset() != null){
						XMLOffset xmlOffset = fac.createXMLSignalPlanTypeXMLOffset();
						xmlPd.setOffset(xmlOffset);
						xmlOffset.setSec(planData.getOffset());
					}
					
					//process signalGroupSettings
					for (SignalGroupSettingsData sgSettings : planData.getSignalGroupSettingsDataByGroupId().values()){
						XMLSignalGroupSettingsType xmlSgSettings = fac.createXMLSignalGroupSettingsType();
						xmlPd.getSignalGroupSettings().add(xmlSgSettings);
						xmlSgSettings.setRefId(sgSettings.getSignalGroupId().toString());
						
						XMLOnset xmlOnset = fac.createXMLSignalGroupSettingsTypeXMLOnset();
						xmlSgSettings.setOnset(xmlOnset);
						xmlOnset.setSec(Integer.valueOf(sgSettings.getOnset()));
						
						XMLDropping xmlDropping = fac.createXMLSignalGroupSettingsTypeXMLDropping();
						xmlSgSettings.setDropping(xmlDropping);
						xmlDropping.setSec(Integer.valueOf(sgSettings.getDropping()));
					}
				}
			} //end process plans
		}
		return xmlSignalControl;
	}
	
	public void write(String filename, XMLSignalControl xmlSignalControl) {
		log.info("writing file: " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalcontrol20.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(MatsimSignalSystemsReader.SIGNALCONTROL20, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(xmlSignalControl, bufout);
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
		XMLSignalControl xmlSignalControl;
		try {
			xmlSignalControl = convertDataToXml();
			this.write(filename, xmlSignalControl);
		} catch (DatatypeConfigurationException e) {
			log.error("Could not write file " + filename + " due to ");
			e.printStackTrace();
		}
	}
	
	private XMLGregorianCalendar getXmlGregorianCalendar(double seconds) throws DatatypeConfigurationException {
		XMLGregorianCalendar time = DatatypeFactory.newInstance().newXMLGregorianCalendar();
		int s = (int) seconds;
		int h = (s / 3600);
		s = s % 3600;
		int m = (s / 60);
		s = s % 60;
		time.setSecond(s);
		time.setMinute(m);
		time.setHour(h);
		return time;
	}
	
}
