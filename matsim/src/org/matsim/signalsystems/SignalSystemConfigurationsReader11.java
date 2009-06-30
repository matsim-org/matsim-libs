/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemConfigurationsReader11
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

import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptivePlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLAdaptiveSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLIdRefType;
import org.matsim.jaxb.signalsystemsconfig11.XMLPlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfig;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfigurationType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType;
import org.matsim.signalsystems.config.BasicAdaptivePlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsBuilder;
import org.matsim.signalsystems.config.BasicSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;

import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalSystemConfigurationsReader11 extends MatsimJaxbXmlParser {

  private XMLSignalSystemConfig xmlLssConfig;
	
  private BasicSignalSystemConfigurationsBuilder builder;

	private BasicSignalSystemConfigurations lssConfigurations;
  
	public SignalSystemConfigurationsReader11(BasicSignalSystemConfigurations lssConfigs, String schemaLocation) {
		super(schemaLocation);
		this.lssConfigurations = lssConfigs;
		this.builder = this.lssConfigurations.getBuilder();
	}
	
	@Override
	public void readFile(final String filename) throws ParserConfigurationException, IOException, JAXBException, SAXException {
  	JAXBContext jc;
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystemsconfig11.ObjectFactory.class);
//			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			//validate file
			super.validateFile(filename, u);
			
			xmlLssConfig = (XMLSignalSystemConfig)u.unmarshal( 
					new FileInputStream( filename ) );

		//convert the parsed xml-instances to basic instances
			for (XMLSignalSystemConfigurationType xmlLssConfiguration : xmlLssConfig.getSignalSystemConfiguration()){
				BasicSignalSystemConfiguration blssc = builder.createSignalSystemConfiguration(new IdImpl(xmlLssConfiguration.getRefId()));
				
				XMLSignalSystemControlInfoType xmlcit = xmlLssConfiguration.getSignalSystemControlInfo();
				BasicSignalSystemControlInfo controlInfo;
				controlInfo = convertXmlControlInfoToBasic(xmlcit);
				blssc.setSignalSystemControlInfo(controlInfo);
				
				this.lssConfigurations.getSignalSystemConfigurations().put(blssc.getSignalSystemId(), blssc);
			} // end outer for
	}

	private BasicSignalSystemControlInfo convertXmlControlInfoToBasic(
			XMLSignalSystemControlInfoType xmlcit) {
		BasicSignalSystemControlInfo controlInfo = null;
		if (xmlcit instanceof XMLPlanbasedSignalSystemControlInfoType) {
			XMLPlanbasedSignalSystemControlInfoType xmlpcit = (XMLPlanbasedSignalSystemControlInfoType) xmlcit;
			
			BasicPlanBasedSignalSystemControlInfo pcontrolInfo = builder.createPlanBasedSignalSystemControlInfo();
			controlInfo = pcontrolInfo;
			
			for (XMLSignalSystemPlanType xmlplan : xmlpcit.getSignalSystemPlan()) {
				pcontrolInfo.addPlan(convertXmlPlanToBasic(xmlplan));
			}
		}
		else if (xmlcit instanceof XMLAdaptivePlanbasedSignalSystemControlInfoType){
			XMLAdaptivePlanbasedSignalSystemControlInfoType sscit = (XMLAdaptivePlanbasedSignalSystemControlInfoType)xmlcit;
			BasicAdaptivePlanBasedSignalSystemControlInfo aci = builder.createAdaptivePlanbasedSignalSystemControlInfo();
			controlInfo = aci;
			aci.setAdaptiveControlerClass(sscit.getAdaptiveControler());
			for (XMLIdRefType idref : sscit.getSignalGroup()){
				aci.addSignalGroupId(new IdImpl(idref.getRefId()));
			}
			for (XMLSignalSystemPlanType xmlplan : sscit.getSignalSystemPlan()) {
				aci.addPlan(convertXmlPlanToBasic(xmlplan));
			}
		}
		else if (xmlcit instanceof XMLAdaptiveSignalSystemControlInfoType) {
			XMLAdaptiveSignalSystemControlInfoType sscit = (XMLAdaptiveSignalSystemControlInfoType)xmlcit;
			BasicAdaptiveSignalSystemControlInfo aci = builder.createAdaptiveSignalSystemControlInfo();
			controlInfo = aci;
			aci.setAdaptiveControlerClass(sscit.getAdaptiveControler());
			for (XMLIdRefType idref : sscit.getSignalGroup()){
				aci.addSignalGroupId(new IdImpl(idref.getRefId()));
			}
		}
		return controlInfo;

	}
	
	private BasicSignalSystemPlan convertXmlPlanToBasic(XMLSignalSystemPlanType xmlplan){
		BasicSignalSystemPlan plan = builder.createSignalSystemPlan(new IdImpl(xmlplan.getId()));					
		plan.setStartTime(getSeconds(xmlplan.getStart().getDaytime()));
		plan.setEndTime(getSeconds(xmlplan.getStop().getDaytime()));
		if (xmlplan.getCycleTime() != null) {
			plan.setCycleTime(xmlplan.getCycleTime().getSec());
		}
		if (xmlplan.getSynchronizationOffset() != null) {
			plan.setSynchronizationOffset(xmlplan.getSynchronizationOffset().getSec());
		}
		for (XMLSignalGroupSettingsType xmlgroupconfig : xmlplan.getSignalGroupSettings()) {
			BasicSignalGroupSettings groupConfig = builder.createSignalGroupSettings(new IdImpl(xmlgroupconfig.getRefId()));
			groupConfig.setRoughCast(xmlgroupconfig.getRoughcast().getSec());
			groupConfig.setDropping(xmlgroupconfig.getDropping().getSec());
			if (xmlgroupconfig.getInterGreenTimeRoughcast() != null)
				groupConfig.setInterGreenTimeRoughcast(xmlgroupconfig.getInterGreenTimeRoughcast().getSec());
			if (xmlgroupconfig.getInterGreenTimeDropping() != null)
				groupConfig.setInterGreenTimeDropping(xmlgroupconfig.getInterGreenTimeDropping().getSec());
			
			plan.addLightSignalGroupConfiguration(groupConfig);
		}		
		return plan;
	}
	
	

	private double getSeconds(XMLGregorianCalendar daytime) {
		double sec = daytime.getHour() * 3600.0;
		sec += daytime.getMinute() * 60.0;
		sec += daytime.getSecond();
		return sec;
	}

}
