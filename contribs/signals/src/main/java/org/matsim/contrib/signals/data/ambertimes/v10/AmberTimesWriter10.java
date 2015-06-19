/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.signals.data.ambertimes.v10;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.amberTimes10.ObjectFactory;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem.XMLSignal;
import org.matsim.jaxb.amberTimes10.XMLAmberTimesType;
import org.matsim.jaxb.amberTimes10.XMLAmberTimesType.XMLAmber;
import org.matsim.jaxb.amberTimes10.XMLAmberTimesType.XMLRedAmber;
import org.matsim.jaxb.amberTimes10.XMLGlobalDefaultsType;
import org.matsim.jaxb.amberTimes10.XMLGlobalDefaultsType.XMLAmberTimeGreen;
import org.matsim.contrib.signals.model.Signal;


/**
 * @author jbischoff
 * @author dgrether
 */
public class AmberTimesWriter10 extends MatsimJaxbXmlWriter {
	
		private static final Logger log = Logger.getLogger(AmberTimesWriter10.class);
		
		private AmberTimesData amberTimesData;
	
		public static final String AMBERTIMES10 = "http://www.matsim.org/files/dtd/amberTimes_v1.0.xsd";
		
		public AmberTimesWriter10(AmberTimesData amberTimesData){
			this.amberTimesData = amberTimesData;
		}
		
		private XMLAmberTimes convertDataToXml() {
			ObjectFactory fac = new ObjectFactory();
			XMLAmberTimes xmlContainer = fac.createXMLAmberTimes();
			
			//global defaults
			XMLGlobalDefaultsType gdt = fac.createXMLGlobalDefaultsType();
			if (this.amberTimesData.getDefaultAmber() != null){
				XMLAmber gda = fac.createXMLAmberTimesTypeXMLAmber();
				gda.setSeconds(BigInteger.valueOf(this.amberTimesData.getDefaultAmber()));
				gdt.setAmber(gda);
			}
			if (this.amberTimesData.getDefaultRedAmber() != null){
				XMLRedAmber gdra = fac.createXMLAmberTimesTypeXMLRedAmber();
				gdra.setSeconds(BigInteger.valueOf(this.amberTimesData.getDefaultRedAmber()));
				gdt.setRedAmber(gdra);
			}
			if (this.amberTimesData.getDefaultAmberTimeGreen() != null){
				XMLAmberTimeGreen gdatg = fac.createXMLGlobalDefaultsTypeXMLAmberTimeGreen();
				gdatg.setProportion(BigDecimal.valueOf(this.amberTimesData.getDefaultAmberTimeGreen()));
				gdt.setAmberTimeGreen(gdatg);
			}
			xmlContainer.setGlobalDefaults(gdt);
			
			for (AmberTimeData atdata : this.amberTimesData.getAmberTimeDataBySystemId().values()){
							
				XMLSignalSystem xmlss = fac.createXMLAmberTimesXMLSignalSystem();
				xmlss.setRefId(atdata.getSignalSystemId().toString());
							
				//Signal System Defaults
				XMLAmberTimesType ssd = fac.createXMLAmberTimesType();	
				if (atdata.getDefaultAmber() != null){
					XMLAmber ssda = fac.createXMLAmberTimesTypeXMLAmber();
					ssda.setSeconds(BigInteger.valueOf(atdata.getDefaultAmber()));
					ssd.setAmber(ssda);
				}
				if (atdata.getDefaultRedAmber() != null){
					XMLRedAmber ssdra = fac.createXMLAmberTimesTypeXMLRedAmber();
					ssdra.setSeconds(BigInteger.valueOf(atdata.getDefaultRedAmber()));
					ssd.setRedAmber(ssdra);
				}
				xmlss.setSystemDefaults(ssd);
				
				for (Map.Entry<Id<Signal> ,Integer> amt : atdata.getSignalAmberMap().entrySet()){
					XMLAmber lca = fac.createXMLAmberTimesTypeXMLAmber();
					XMLRedAmber lcra = fac.createXMLAmberTimesTypeXMLRedAmber();
					XMLSignal xmls = fac.createXMLAmberTimesXMLSignalSystemXMLSignal();
					xmls.setRefId(amt.getKey().toString());
					lca.setSeconds(BigInteger.valueOf(amt.getValue()));
					lcra.setSeconds(BigInteger.valueOf(atdata.getRedAmberOfSignal(amt.getKey())));
					xmls.setAmber(lca);
					xmls.setRedAmber(lcra);
					xmlss.getSignal().add(xmls);
					
				}		
				xmlContainer.getSignalSystem().add(xmlss);
			}
			return xmlContainer;
		}

		public void write(String filename, XMLAmberTimes xmlAmberTimes) {
			log.info("writing file: " + filename);
	  	JAXBContext jc;
			try {
				jc = JAXBContext.newInstance(org.matsim.jaxb.amberTimes10.ObjectFactory.class);
				Marshaller m = jc.createMarshaller();
				super.setMarshallerProperties(AmberTimesWriter10.AMBERTIMES10, m);
				BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
				m.marshal(xmlAmberTimes, bufout);
				bufout.close();
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
			XMLAmberTimes xmlAmberTimes = convertDataToXml();
			this.write(filename, xmlAmberTimes);
		}

		
	}

