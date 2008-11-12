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

package org.matsim.lightsignalsystems;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.lightsignalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.xml.lightsignalsystemsconfig.ObjectFactory;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalGroupConfigurationType;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalSystemConfig;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalSystemConfigurationType;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalSystemPlanType;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLMatsimTimeAttributeType;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLPlanbasedlightSignalSystemControlInfoType;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalGroupConfigurationType.XMLInterimTimeDropping;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalSystemPlanType.XMLStart;
import org.matsim.basic.xml.lightsignalsystemsconfig.XMLLightSignalSystemPlanType.XMLStop;
import org.matsim.utils.io.IOUtils;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * @author dgrether
 */
public class MatsimLightSignalSystemConfigurationWriter {

	private List<BasicLightSignalSystemConfiguration> blssconfs;
	private XMLLightSignalSystemConfig xmllssconfig;

	public MatsimLightSignalSystemConfigurationWriter(List<BasicLightSignalSystemConfiguration> basiclssconfigs) {
		this.blssconfs = basiclssconfigs;
		this.xmllssconfig = convertBasicToXml();
	}
	
	
	
	public void writeFile(final String filename) {
  	JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.basic.xml.lightsignalsystemsconfig.ObjectFactory.class);
			Marshaller m = jc.createMarshaller(); 
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, 
					Boolean.TRUE); 
			
			m.marshal(this.xmllssconfig, IOUtils.getBufferedWriter(filename)); 
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private XMLLightSignalSystemConfig convertBasicToXml() {
		ObjectFactory fac = new ObjectFactory();
		XMLLightSignalSystemConfig xmllssconf = fac.createXMLLightSignalSystemConfig();
		
		for (BasicLightSignalSystemConfiguration lssconf : this.blssconfs) {
			XMLLightSignalSystemConfigurationType xmllssconfiguration = fac.createXMLLightSignalSystemConfigurationType();
			xmllssconfiguration.setRefId(lssconf.getLightSignalSystemId().toString());
			
			if (lssconf.getControlInfo() instanceof BasicPlanBasedLightSignalSystemControlInfo) {
				XMLPlanbasedlightSignalSystemControlInfoType xmlplanlsscontrolinfo = fac.createXMLPlanbasedlightSignalSystemControlInfoType();
				BasicPlanBasedLightSignalSystemControlInfo pbcontrolinfo = (BasicPlanBasedLightSignalSystemControlInfo) lssconf.getControlInfo();
				for (BasicLightSignalSystemPlan plan : pbcontrolinfo.getPlans().values()) {
					XMLLightSignalSystemPlanType xmlplan = fac.createXMLLightSignalSystemPlanType();
					xmlplan.setId(plan.getId().toString());
					XMLStart start = new XMLStart();
					start.setDaytime(getXmlGregorianCalendar(plan.getStartTime()));
					xmlplan.setStart(start);
					
					XMLStop stop = new XMLStop();
					stop.setDaytime(getXmlGregorianCalendar(plan.getEndTime()));
					xmlplan.setStop(stop);
					
					XMLMatsimTimeAttributeType xmlct = fac.createXMLMatsimTimeAttributeType();
					if (plan.getCirculationTime() != null) {
						xmlct.setSeconds(plan.getCirculationTime());
						xmlplan.setCirculationTime(xmlct);
					}
					if (plan.getSyncronizationOffset() != null) {
						XMLMatsimTimeAttributeType xmlso = fac.createXMLMatsimTimeAttributeType();
						xmlso.setSeconds(plan.getSyncronizationOffset());
						xmlplan.setSyncronizationOffset(xmlso);
					}
					
					

					//write lightSignalGroupConfigurations
					for (BasicLightSignalGroupConfiguration lsgc : plan.getGroupConfigs().values()) {
						XMLLightSignalGroupConfigurationType xmllsgc = fac.createXMLLightSignalGroupConfigurationType();
						xmllsgc.setRefId(lsgc.getReferencedSignalGroupId().toString());
						XMLLightSignalGroupConfigurationType.XMLRoughcast xmlrc = new XMLLightSignalGroupConfigurationType.XMLRoughcast();
						//FIXME change in dataformat from int to double
						xmlrc.setSec((int)lsgc.getRoughCast());
						xmllsgc.setRoughcast(xmlrc);
						
						XMLLightSignalGroupConfigurationType.XMLDropping xmldropping = new XMLLightSignalGroupConfigurationType.XMLDropping();
						xmldropping.setSec((int)lsgc.getDropping());
						xmllsgc.setDropping(xmldropping);
						if (lsgc.getInterimTimeDropping() != null) {
							XMLLightSignalGroupConfigurationType.XMLInterimTimeDropping xmlitd = new XMLInterimTimeDropping();
							xmlitd.setSec((int) lsgc.getInterimTimeDropping().doubleValue());
							xmllsgc.setInterimTimeDropping(xmlitd);
						}

						if (lsgc.getInterimTimeRoughcast() != null) {
							XMLLightSignalGroupConfigurationType.XMLInterimTimeRoughcast xmlitr = new XMLLightSignalGroupConfigurationType.XMLInterimTimeRoughcast();
							xmlitr.setSec((int) lsgc.getInterimTimeRoughcast().doubleValue());
							xmllsgc.setInterimTimeRoughcast(xmlitr);
						}
						
						xmlplan.getLightSignalGroupConfiguration().add(xmllsgc);
					}
					xmlplanlsscontrolinfo.getLightSignalSystemPlan().add(xmlplan);
				}
				xmllssconfiguration.setLightSignalSystemControlInfo(xmlplanlsscontrolinfo);
			}
			else {
			//TODO implement adaptive control
				throw new UnsupportedOperationException("has to be implemented!");
			}
			xmllssconf.getLightSignalSystemConfiguration().add(xmllssconfiguration);
		}
		return xmllssconf;
	}



	private XMLGregorianCalendar getXmlGregorianCalendar(double seconds) {
		XMLGregorianCalendar time = new XMLGregorianCalendarImpl();
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
