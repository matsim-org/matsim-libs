/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.core.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurationsBuilder;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLAdaptiveLightSignalSystemControlInfoType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalGroupConfigurationType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemConfig;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemConfigurationType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemControlInfoType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemPlanType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLPlanbasedlightSignalSystemControlInfoType;
import org.xml.sax.SAXException;

/**
 * Reader for the lightSignalSystemConfigurations_v1.0 file format.
 * @author dgrether
 * @deprecated use singalSystemConfigurations_v1.1 reader instead
 */
@Deprecated
public class LightSignalSystemConfigurationsReader10 extends MatsimJaxbXmlParser {

  private XMLLightSignalSystemConfig xmlLssConfig;
	
  private BasicSignalSystemConfigurationsBuilder builder;

	private BasicSignalSystemConfigurations lssConfigurations;
  
	public LightSignalSystemConfigurationsReader10(BasicSignalSystemConfigurations lssConfigs, String schemaLocation) {
		super(schemaLocation);
		this.lssConfigurations = lssConfigs;
		this.builder = lssConfigs.getBuilder();
	}
	
	@Override
	public void readFile(final String filename) throws ParserConfigurationException, IOException, JAXBException, SAXException {
  	JAXBContext jc;
			jc = JAXBContext.newInstance(org.matsim.jaxb.lightsignalsystemsconfig10.ObjectFactory.class);
//			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			//validate file
			super.validateFile(filename, u);
			
			xmlLssConfig = (XMLLightSignalSystemConfig)u.unmarshal( 
					new FileInputStream( filename ) );

		//convert the parsed xml-instances to basic instances
			for (XMLLightSignalSystemConfigurationType xmlLssConfiguration : xmlLssConfig.getLightSignalSystemConfiguration()){
				BasicSignalSystemConfiguration blssc = builder.createSignalSystemConfiguration(new IdImpl(xmlLssConfiguration.getRefId()));
				
				XMLLightSignalSystemControlInfoType xmlcit = xmlLssConfiguration.getLightSignalSystemControlInfo();
				if (xmlcit instanceof XMLPlanbasedlightSignalSystemControlInfoType) {
					XMLPlanbasedlightSignalSystemControlInfoType xmlpcit = (XMLPlanbasedlightSignalSystemControlInfoType) xmlcit;
					
					BasicPlanBasedSignalSystemControlInfo controlInfo = builder.createPlanBasedSignalSystemControlInfo();
					
					for (XMLLightSignalSystemPlanType xmlplan : xmlpcit.getLightSignalSystemPlan()) {
						BasicSignalSystemPlan plan = builder.createSignalSystemPlan(new IdImpl(xmlplan.getId()));					
						plan.setStartTime(getSeconds(xmlplan.getStart().getDaytime()));
						plan.setEndTime(getSeconds(xmlplan.getStop().getDaytime()));
						if (xmlplan.getCirculationTime() != null) {
							plan.setCirculationTime((int)xmlplan.getCirculationTime().getSeconds());
						}
						if (xmlplan.getSyncronizationOffset() != null) {
							plan.setSyncronizationOffset((int)xmlplan.getSyncronizationOffset().getSeconds());
						}
						for (XMLLightSignalGroupConfigurationType xmlgroupconfig : xmlplan.getLightSignalGroupConfiguration()) {
							BasicSignalGroupSettings groupConfig = builder.createSignalGroupSettings(new IdImpl(xmlgroupconfig.getRefId()));
							groupConfig.setRoughCast(xmlgroupconfig.getRoughcast().getSec());
							groupConfig.setDropping(xmlgroupconfig.getDropping().getSec());
							if (xmlgroupconfig.getInterimTimeRoughcast() != null)
								groupConfig.setInterimTimeRoughcast(xmlgroupconfig.getInterimTimeRoughcast().getSec());
							if (xmlgroupconfig.getInterimTimeDropping() != null)
								groupConfig.setInterimTimeDropping(xmlgroupconfig.getInterimTimeDropping().getSec());
							
							plan.addLightSignalGroupConfiguration(groupConfig);
						}
						controlInfo.addPlan(plan);
						
					}
					blssc.setSignalSystemControlInfo(controlInfo);
				}
				else if (xmlcit instanceof XMLAdaptiveLightSignalSystemControlInfoType) {
					throw new UnsupportedOperationException("Implemented in version 1.1 of data format, please convert!");
				}
				
				this.lssConfigurations.getSignalSystemConfigurations().put(blssc.getSignalSystemId(), blssc);
			} // end outer for
	}

	private double getSeconds(XMLGregorianCalendar daytime) {
		double sec = daytime.getHour() * 3600.0;
		sec += daytime.getMinute() * 60.0;
		sec += daytime.getSecond();
		return sec;
	}
	
}
