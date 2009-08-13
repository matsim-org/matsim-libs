/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemConfigurationsWriter11
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.signalsystemsconfig11.ObjectFactory;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptivePlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptiveSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLIdRefType;
import org.matsim.jaxb.signalsystemsconfig11.XMLPlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfig;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfigurationType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType.XMLInterGreenTimeDropping;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLCycleTime;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLPowerOffTime;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLPowerOnTime;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLStart;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLStop;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLSynchronizationOffset;
import org.matsim.signalsystems.config.BasicAdaptivePlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;


public class SignalSystemConfigurationsWriter11 extends MatsimJaxbXmlWriter{

	
	private BasicSignalSystemConfigurations blssconfs;
	private XMLSignalSystemConfig xmllssconfig;

	public SignalSystemConfigurationsWriter11(BasicSignalSystemConfigurations basiclssconfigs) {
		this.blssconfs = basiclssconfigs;
		try {
			this.xmllssconfig = convertBasicToXml();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void writeFile(final String filename) {
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystemsconfig11.ObjectFactory.class);
			Marshaller m = jc.createMarshaller(); 
			super.setMarshallerProperties(MatsimSignalSystemConfigurationsReader.SIGNALSYSTEMSCONFIG11, m);
			m.marshal(this.xmllssconfig, IOUtils.getBufferedWriter(filename)); 
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private XMLSignalSystemConfig convertBasicToXml() throws DatatypeConfigurationException {
		ObjectFactory fac = new ObjectFactory();
		XMLSignalSystemConfig xmllssconf = fac.createXMLSignalSystemConfig();
		
		for (BasicSignalSystemConfiguration lssconf : this.blssconfs.getSignalSystemConfigurations().values()) {
			XMLSignalSystemConfigurationType xmllssconfiguration = fac.createXMLSignalSystemConfigurationType();
			xmllssconfiguration.setRefId(lssconf.getSignalSystemId().toString());
			
			XMLSignalSystemControlInfoType xmlControlInfo = convertBasicControlInfoToXml(lssconf.getControlInfo(), fac);

			xmllssconfiguration.setSignalSystemControlInfo(xmlControlInfo);
			xmllssconf.getSignalSystemConfiguration().add(xmllssconfiguration);
		}
		return xmllssconf;
	}

	private XMLSignalSystemControlInfoType convertBasicControlInfoToXml(BasicSignalSystemControlInfo controlInfo, ObjectFactory fac) throws DatatypeConfigurationException {
		XMLSignalSystemControlInfoType control = null;
		if (controlInfo instanceof BasicAdaptivePlanBasedSignalSystemControlInfo){
			XMLAdaptivePlanbasedSignalSystemControlInfoType xmladaptivepbcontrolinfo = fac.createXMLAdaptivePlanbasedSignalSystemControlInfoType();
			control = xmladaptivepbcontrolinfo;
			BasicAdaptivePlanBasedSignalSystemControlInfo adaptivepbcontrolinfo = (BasicAdaptivePlanBasedSignalSystemControlInfo) controlInfo ;
			xmladaptivepbcontrolinfo.setAdaptiveControler(adaptivepbcontrolinfo.getAdaptiveControlerClass());
			for (Id id :  adaptivepbcontrolinfo.getSignalGroupIds()){
				XMLIdRefType xmlid = new XMLIdRefType();
				xmlid.setRefId(id.toString());
				xmladaptivepbcontrolinfo.getSignalGroup().add(xmlid);
			}
			for (BasicSignalSystemPlan plan : adaptivepbcontrolinfo.getPlans().values()) {
				XMLSignalSystemPlanType xmlplan = this.convertBasicPlanToXmlPlan(plan, fac);
				xmladaptivepbcontrolinfo.getSignalSystemPlan().add(xmlplan);
			}
		}
		else if (controlInfo instanceof BasicPlanBasedSignalSystemControlInfo) {
			XMLPlanbasedSignalSystemControlInfoType xmlplanlsscontrolinfo = fac.createXMLPlanbasedSignalSystemControlInfoType();
			control = xmlplanlsscontrolinfo;
			BasicPlanBasedSignalSystemControlInfo pbcontrolinfo = (BasicPlanBasedSignalSystemControlInfo) controlInfo;
			for (BasicSignalSystemPlan plan : pbcontrolinfo.getPlans().values()) {
				XMLSignalSystemPlanType xmlplan = this.convertBasicPlanToXmlPlan(plan, fac);
				xmlplanlsscontrolinfo.getSignalSystemPlan().add(xmlplan);
			}
		}
		else if (controlInfo instanceof BasicAdaptiveSignalSystemControlInfo){
			XMLAdaptiveSignalSystemControlInfoType xmladaptivecontrolinfo = fac.createXMLAdaptiveSignalSystemControlInfoType();
			control = xmladaptivecontrolinfo;
			BasicAdaptiveSignalSystemControlInfo adaptivecontrolinfo = (BasicAdaptiveSignalSystemControlInfo) controlInfo ;
			xmladaptivecontrolinfo.setAdaptiveControler(adaptivecontrolinfo.getAdaptiveControlerClass());
			for (Id id :  adaptivecontrolinfo.getSignalGroupIds()){
				XMLIdRefType xmlid = new XMLIdRefType();
				xmlid.setRefId(id.toString());
				xmladaptivecontrolinfo.getSignalGroup().add(xmlid);
			}
			
		}
		return control;
	}
	
	

	private XMLSignalSystemPlanType convertBasicPlanToXmlPlan(
			BasicSignalSystemPlan plan, ObjectFactory fac) throws DatatypeConfigurationException {
		XMLSignalSystemPlanType xmlplan = fac.createXMLSignalSystemPlanType();
		xmlplan.setId(plan.getId().toString());
		XMLStart start = new XMLStart();
		start.setDaytime(getXmlGregorianCalendar(plan.getStartTime()));
		xmlplan.setStart(start);
		
		XMLStop stop = new XMLStop();
		stop.setDaytime(getXmlGregorianCalendar(plan.getEndTime()));
		xmlplan.setStop(stop);
		
		XMLPowerOnTime on = new XMLPowerOnTime();
		on.setSec(plan.getPowerOnTime());
		xmlplan.setPowerOnTime(on);
		
		XMLPowerOffTime off = new XMLPowerOffTime();
		off.setSec(plan.getPowerOffTime());
		xmlplan.setPowerOffTime(off);
		
		XMLCycleTime xmlct = new XMLCycleTime();
		if (plan.getCycleTime() != null) {
			xmlct.setSec(plan.getCycleTime());
			xmlplan.setCycleTime(xmlct);
		}
		if (plan.getSynchronizationOffset() != null) {
			XMLSynchronizationOffset xmlso = new XMLSynchronizationOffset();
			xmlso.setSec(plan.getSynchronizationOffset());
			xmlplan.setSynchronizationOffset(xmlso);
		}
		
		//marshal SignalGroupConfigurations
		for (BasicSignalGroupSettings lsgc : plan.getGroupConfigs().values()) {
			XMLSignalGroupSettingsType xmllsgc = fac.createXMLSignalGroupSettingsType();
			xmllsgc.setRefId(lsgc.getReferencedSignalGroupId().toString());
			XMLSignalGroupSettingsType.XMLRoughcast xmlrc = new XMLSignalGroupSettingsType.XMLRoughcast();
			xmlrc.setSec((int)lsgc.getRoughCast());
			xmllsgc.setRoughcast(xmlrc);
			
			XMLSignalGroupSettingsType.XMLDropping xmldropping = new XMLSignalGroupSettingsType.XMLDropping();
			xmldropping.setSec((int)lsgc.getDropping());
			xmllsgc.setDropping(xmldropping);
			if (lsgc.getInterGreenTimeDropping() != null) {
				XMLSignalGroupSettingsType.XMLInterGreenTimeDropping xmlitd = new XMLInterGreenTimeDropping();
				xmlitd.setSec((int) lsgc.getInterGreenTimeDropping().doubleValue());
				xmllsgc.setInterGreenTimeDropping(xmlitd);
			}

			if (lsgc.getInterimGreenTimeRoughcast() != null) {
				XMLSignalGroupSettingsType.XMLInterGreenTimeRoughcast xmlitr = new XMLSignalGroupSettingsType.XMLInterGreenTimeRoughcast();
				xmlitr.setSec((int) lsgc.getInterimGreenTimeRoughcast().doubleValue());
				xmllsgc.setInterGreenTimeRoughcast(xmlitr);
			}
			
			xmlplan.getSignalGroupSettings().add(xmllsgc);
		}
		return xmlplan;
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
