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

import org.matsim.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.basic.signalsystemsconfig.BasicSignalGroupSettings;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.jaxb.signalsystemsconfig11.ObjectFactory;
import org.matsim.jaxb.signalsystemsconfig11.XMLPlanbasedSignalSystemControlInfoType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfig;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfigurationType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalGroupSettingsType.XMLInterimTimeDropping;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLCirculationTime;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLStart;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLStop;
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemPlanType.XMLSyncronizationOffset;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.io.MatsimJaxbXmlWriter;

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
			
			if (lssconf.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo) {
				XMLPlanbasedSignalSystemControlInfoType xmlplanlsscontrolinfo = fac.createXMLPlanbasedSignalSystemControlInfoType();
				BasicPlanBasedSignalSystemControlInfo pbcontrolinfo = (BasicPlanBasedSignalSystemControlInfo) lssconf.getControlInfo();
				for (BasicSignalSystemPlan plan : pbcontrolinfo.getPlans().values()) {
					XMLSignalSystemPlanType xmlplan = fac.createXMLSignalSystemPlanType();
					xmlplan.setId(plan.getId().toString());
					XMLStart start = new XMLStart();
					start.setDaytime(getXmlGregorianCalendar(plan.getStartTime()));
					xmlplan.setStart(start);
					
					XMLStop stop = new XMLStop();
					stop.setDaytime(getXmlGregorianCalendar(plan.getEndTime()));
					xmlplan.setStop(stop);
					
					XMLCirculationTime xmlct = new XMLCirculationTime();
					if (plan.getCirculationTime() != null) {
						xmlct.setSec(plan.getCirculationTime());
						xmlplan.setCirculationTime(xmlct);
					}
					if (plan.getSyncronizationOffset() != null) {
						XMLSyncronizationOffset xmlso = new XMLSyncronizationOffset();
						xmlso.setSec(plan.getSyncronizationOffset());
						xmlplan.setSyncronizationOffset(xmlso);
					}
					
					

					//write SignalGroupConfigurations
					for (BasicSignalGroupSettings lsgc : plan.getGroupConfigs().values()) {
						XMLSignalGroupSettingsType xmllsgc = fac.createXMLSignalGroupSettingsType();
						xmllsgc.setRefId(lsgc.getReferencedSignalGroupId().toString());
						XMLSignalGroupSettingsType.XMLRoughcast xmlrc = new XMLSignalGroupSettingsType.XMLRoughcast();
						xmlrc.setSec((int)lsgc.getRoughCast());
						xmllsgc.setRoughcast(xmlrc);
						
						XMLSignalGroupSettingsType.XMLDropping xmldropping = new XMLSignalGroupSettingsType.XMLDropping();
						xmldropping.setSec((int)lsgc.getDropping());
						xmllsgc.setDropping(xmldropping);
						if (lsgc.getInterimTimeDropping() != null) {
							XMLSignalGroupSettingsType.XMLInterimTimeDropping xmlitd = new XMLInterimTimeDropping();
							xmlitd.setSec((int) lsgc.getInterimTimeDropping().doubleValue());
							xmllsgc.setInterimTimeDropping(xmlitd);
						}

						if (lsgc.getInterimTimeRoughcast() != null) {
							XMLSignalGroupSettingsType.XMLInterimTimeRoughcast xmlitr = new XMLSignalGroupSettingsType.XMLInterimTimeRoughcast();
							xmlitr.setSec((int) lsgc.getInterimTimeRoughcast().doubleValue());
							xmllsgc.setInterimTimeRoughcast(xmlitr);
						}
						
						xmlplan.getSignalGroupSettings().add(xmllsgc);
					}
					xmlplanlsscontrolinfo.getSignalSystemPlan().add(xmlplan);
				}
				xmllssconfiguration.setSignalSystemControlInfo(xmlplanlsscontrolinfo);
			}
			else {
				throw new UnsupportedOperationException("Implemented in v1.1 version of data format, please convert your files!");
			}
			xmllssconf.getSignalSystemConfiguration().add(xmllssconfiguration);
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
