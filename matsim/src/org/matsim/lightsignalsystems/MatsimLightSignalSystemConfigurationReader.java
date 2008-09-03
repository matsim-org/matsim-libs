/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.lightsignalsystems;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemsConfigFactory;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemÇonfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLAdaptiveLightSignalSystemControlInfoType;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLLightSignalGroupConfigurationType;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLLightSignalSystemConfig;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLLightSignalSystemConfigurationType;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLLightSignalSystemControlInfoType;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLLightSignalSystemPlanType;
import org.matsim.basic.lightsignalsystemsconfig.xml.XMLPlanbasedlightSignalSystemControlInfoType;
import org.matsim.basic.v01.IdImpl;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class MatsimLightSignalSystemConfigurationReader {

  private XMLLightSignalSystemConfig xmlLssConfig;
	
  private BasicLightSignalSystemsConfigFactory factory = new BasicLightSignalSystemsConfigFactory();

	private List<BasicLightSignalSystemÇonfiguration> lssConfigurations;
  
	public MatsimLightSignalSystemConfigurationReader(List<BasicLightSignalSystemÇonfiguration> lssConfigs) {
		this.lssConfigurations = lssConfigs;
	}
	
	public void readFile(final String filename) {
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.basic.lightsignalsystemsconfig.xml.ObjectFactory.class);
//			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			Schema schema = schemaFac.newSchema(new URL("http://www.matsim.org/files/dtd/lightSignalSystemsConfig_v1.0.xsd"));
			u.setSchema(schema);
			xmlLssConfig = (XMLLightSignalSystemConfig)u.unmarshal( 
					new FileInputStream( filename ) );
			
			for (XMLLightSignalSystemConfigurationType xmlLssConfiguration : xmlLssConfig.getLightSignalSystemConfiguration()){
				BasicLightSignalSystemÇonfiguration blssc = factory.createLightSignalSystemConfiguration(new IdImpl(xmlLssConfiguration.getRefId()));
				
				XMLLightSignalSystemControlInfoType xmlcit = xmlLssConfiguration.getLightSignalSystemControlInfo();
				
				if (xmlcit instanceof XMLPlanbasedlightSignalSystemControlInfoType) {
					XMLPlanbasedlightSignalSystemControlInfoType xmlpcit = (XMLPlanbasedlightSignalSystemControlInfoType) xmlcit;
					
					BasicPlanBasedLightSignalSystemControlInfo controlInfo = factory.createPlanBasedLightSignalSystemControlInfo();
					
					for (XMLLightSignalSystemPlanType xmlplan : xmlpcit.getLightSignalSystemPlan()) {
						BasicLightSignalSystemPlan plan = factory.createLightSignalSystemPlan(new IdImpl(xmlplan.getId()));					
						plan.setStartTime(getSeconds(xmlplan.getStart().getDaytime()));
						plan.setEndTime(getSeconds(xmlplan.getStop().getDaytime()));
						
						for (XMLLightSignalGroupConfigurationType xmlgroupconfig : xmlplan.getLightSignalGroupConfiguration()) {
							BasicLightSignalGroupConfiguration groupConfig = factory.createLightSignalGroupConfiguration(new IdImpl(xmlgroupconfig.getRefId()));
							groupConfig.setRoughCast(xmlgroupconfig.getRoughcast().getSec().doubleValue());
							groupConfig.setDropping(xmlgroupconfig.getDropping().getSec().doubleValue());
							if (xmlgroupconfig.getInterimTimeRoughcast() != null)
								groupConfig.setInterimTimeRoughcast(xmlgroupconfig.getInterimTimeRoughcast().getSec().doubleValue());
							if (xmlgroupconfig.getInterimTimeDropping() != null)
								groupConfig.setInterimTimeDropping(xmlgroupconfig.getInterimTimeDropping().getSec().doubleValue());
							
							plan.addLightSignalGroupConfiguration(groupConfig);
						}
						controlInfo.addPlan(plan);
						
					}
					blssc.setLightSignalSystemControlInfo(controlInfo);
				}
				else if (xmlcit instanceof XMLAdaptiveLightSignalSystemControlInfoType) {
					//TODO implement adaptive control
					throw new UnsupportedOperationException("has to be implemented!");
				}
				
				this.lssConfigurations.add(blssc);
			} // end outer for
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			 e.printStackTrace();
		 } catch (SAXException e) {
			 e.printStackTrace();
		} 
		
		
		
	}

	private double getSeconds(XMLGregorianCalendar daytime) {
		double sec = daytime.getHour() * 3600.0;
		sec += daytime.getMinute() * 60.0;
		sec += daytime.getSecond();
		return sec;
	}
	
}
