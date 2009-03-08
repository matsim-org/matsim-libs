/* *********************************************************************** *
 * project: org.matsim.*
 * LightSignalSystemConfigurationsWriter10
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

import org.matsim.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.basic.signalsystemsconfig.BasicSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.jaxb.lightsignalsystemsconfig10.ObjectFactory;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalGroupConfigurationType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemConfig;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemConfigurationType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemPlanType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLMatsimTimeAttributeType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLPlanbasedlightSignalSystemControlInfoType;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalGroupConfigurationType.XMLInterimTimeDropping;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemPlanType.XMLStart;
import org.matsim.jaxb.lightsignalsystemsconfig10.XMLLightSignalSystemPlanType.XMLStop;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.io.MatsimJaxbXmlWriter;


/**
 * Writer for the lightSignalSystemConfiguration_v1.0.xsd file format.
 * @author dgrether
 *
 */
public class LightSignalSystemConfigurationsWriter10 extends MatsimJaxbXmlWriter{

	public static final String SCHEMALOCATION = "http://www.matsim.org/files/dtd/lightSignalSystemsConfig_v1.0.xsd";
	
	private BasicSignalSystemConfigurations blssconfs;
	private XMLLightSignalSystemConfig xmllssconfig;

	public LightSignalSystemConfigurationsWriter10(BasicSignalSystemConfigurations basiclssconfigs) {
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
			jc = JAXBContext.newInstance(org.matsim.jaxb.lightsignalsystemsconfig10.ObjectFactory.class);
			Marshaller m = jc.createMarshaller(); 
			super.setMarshallerProperties(SCHEMALOCATION, m);
			m.marshal(this.xmllssconfig, IOUtils.getBufferedWriter(filename)); 
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private XMLLightSignalSystemConfig convertBasicToXml() throws DatatypeConfigurationException {
		ObjectFactory fac = new ObjectFactory();
		XMLLightSignalSystemConfig xmllssconf = fac.createXMLLightSignalSystemConfig();
		
		for (BasicSignalSystemConfiguration lssconf : this.blssconfs.getSignalSystemConfigurations().values()) {
			XMLLightSignalSystemConfigurationType xmllssconfiguration = fac.createXMLLightSignalSystemConfigurationType();
			xmllssconfiguration.setRefId(lssconf.getLightSignalSystemId().toString());
			
			if (lssconf.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo) {
				XMLPlanbasedlightSignalSystemControlInfoType xmlplanlsscontrolinfo = fac.createXMLPlanbasedlightSignalSystemControlInfoType();
				BasicPlanBasedSignalSystemControlInfo pbcontrolinfo = (BasicPlanBasedSignalSystemControlInfo) lssconf.getControlInfo();
				for (BasicSignalSystemPlan plan : pbcontrolinfo.getPlans().values()) {
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
					for (BasicSignalGroupConfiguration lsgc : plan.getGroupConfigs().values()) {
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
