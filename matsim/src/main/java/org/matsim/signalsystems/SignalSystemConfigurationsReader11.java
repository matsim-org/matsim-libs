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
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
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
import org.matsim.signalsystems.config.AdaptivePlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalGroupSettings;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsFactory;
import org.matsim.signalsystems.config.SignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalSystemPlan;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 */
public class SignalSystemConfigurationsReader11 extends MatsimJaxbXmlParser {

	private final static Logger log = Logger.getLogger(SignalSystemConfigurationsReader11.class);

  private XMLSignalSystemConfig xmlLssConfig;

  private SignalSystemConfigurationsFactory builder;

	private SignalSystemConfigurations lssConfigurations;

	public SignalSystemConfigurationsReader11(SignalSystemConfigurations lssConfigs, String schemaLocation) {
		super(schemaLocation);
		this.lssConfigurations = lssConfigs;
		this.builder = this.lssConfigurations.getFactory();
	}

	@Override
	public void readFile(final String filename) throws ParserConfigurationException, IOException, JAXBException, SAXException {
  	JAXBContext jc;
			jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystemsconfig11.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			//validate file
			super.validateFile(filename, u);

			InputStream stream = new FileInputStream(filename);
			xmlLssConfig = (XMLSignalSystemConfig)u.unmarshal(stream);
			try {
				stream.close();
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}

		//convert the parsed xml-instances to basic instances
			for (XMLSignalSystemConfigurationType xmlLssConfiguration : xmlLssConfig.getSignalSystemConfiguration()){
				SignalSystemConfiguration blssc = builder.createSignalSystemConfiguration(new IdImpl(xmlLssConfiguration.getRefId()));

				XMLSignalSystemControlInfoType xmlcit = xmlLssConfiguration.getSignalSystemControlInfo();
				SignalSystemControlInfo controlInfo;
				controlInfo = convertXmlControlInfoToBasic(xmlcit);
				blssc.setSignalSystemControlInfo(controlInfo);

				this.lssConfigurations.getSignalSystemConfigurations().put(blssc.getSignalSystemId(), blssc);
			} // end outer for
	}

	private SignalSystemControlInfo convertXmlControlInfoToBasic(
			XMLSignalSystemControlInfoType xmlcit) {
		SignalSystemControlInfo controlInfo = null;
		if (xmlcit instanceof XMLPlanbasedSignalSystemControlInfoType) {
			XMLPlanbasedSignalSystemControlInfoType xmlpcit = (XMLPlanbasedSignalSystemControlInfoType) xmlcit;

			PlanBasedSignalSystemControlInfo pcontrolInfo = builder.createPlanBasedSignalSystemControlInfo();
			controlInfo = pcontrolInfo;

			for (XMLSignalSystemPlanType xmlplan : xmlpcit.getSignalSystemPlan()) {
				pcontrolInfo.addPlan(convertXmlPlanToBasic(xmlplan));
			}
		}
		else if (xmlcit instanceof XMLAdaptivePlanbasedSignalSystemControlInfoType){
			XMLAdaptivePlanbasedSignalSystemControlInfoType sscit = (XMLAdaptivePlanbasedSignalSystemControlInfoType)xmlcit;
			AdaptivePlanBasedSignalSystemControlInfo aci = builder.createAdaptivePlanbasedSignalSystemControlInfo();
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
			AdaptiveSignalSystemControlInfo aci = builder.createAdaptiveSignalSystemControlInfo();
			controlInfo = aci;
			aci.setAdaptiveControlerClass(sscit.getAdaptiveControler());
			for (XMLIdRefType idref : sscit.getSignalGroup()){
				aci.addSignalGroupId(new IdImpl(idref.getRefId()));
			}
		}
		return controlInfo;

	}

	private SignalSystemPlan convertXmlPlanToBasic(XMLSignalSystemPlanType xmlplan){
		SignalSystemPlan plan = builder.createSignalSystemPlan(new IdImpl(xmlplan.getId()));
		plan.setStartTime(getSeconds(xmlplan.getStart().getDaytime()));
		plan.setEndTime(getSeconds(xmlplan.getStop().getDaytime()));
		if (xmlplan.getCycleTime() != null) {
			plan.setCycleTime(xmlplan.getCycleTime().getSec());
		}
		if (xmlplan.getSynchronizationOffset() != null) {
			plan.setSynchronizationOffset(xmlplan.getSynchronizationOffset().getSec());
		}
		if (xmlplan.getPowerOnTime() != null) {
			plan.setPowerOnTime(xmlplan.getPowerOnTime().getSec());
		}
		if (xmlplan.getPowerOffTime() != null) {
			plan.setPowerOffTime(xmlplan.getPowerOffTime().getSec());
		}
		for (XMLSignalGroupSettingsType xmlgroupconfig : xmlplan.getSignalGroupSettings()) {
			SignalGroupSettings groupConfig = builder.createSignalGroupSettings(new IdImpl(xmlgroupconfig.getRefId()));
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
