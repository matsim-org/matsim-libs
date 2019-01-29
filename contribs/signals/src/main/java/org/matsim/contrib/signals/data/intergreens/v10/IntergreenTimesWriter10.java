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
package org.matsim.contrib.signals.data.intergreens.v10;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.jaxb.intergreenTimes10.ObjectFactory;
import org.matsim.jaxb.intergreenTimes10.XMLEndingSignalGroupType;
import org.matsim.jaxb.intergreenTimes10.XMLEndingSignalGroupType.XMLBeginningSignalGroup;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes.XMLSignalSystem;
import org.matsim.contrib.signals.model.SignalGroup;


/**
 * @author dgrether
 */
public class IntergreenTimesWriter10 extends MatsimJaxbXmlWriter {
	
		private static final Logger log = Logger.getLogger(IntergreenTimesWriter10.class);
		
		private IntergreenTimesData intergreensData;
	
		public static final String INTERGREENTIMES10 = "http://www.matsim.org/files/dtd/intergreenTimes_v1.0.xsd";
		
		public IntergreenTimesWriter10(IntergreenTimesData intergreensData){
			this.intergreensData = intergreensData;
		}
		
		private XMLIntergreenTimes convertDataToXml() {
			ObjectFactory fac = new ObjectFactory();
			XMLIntergreenTimes xmlContainer = fac.createXMLIntergreenTimes();
			
			for (IntergreensForSignalSystemData intergreens :  this.intergreensData.getIntergreensForSignalSystemDataMap().values()) {
				XMLSignalSystem xmlss = fac.createXMLIntergreenTimesXMLSignalSystem();
				xmlss.setRefId(intergreens.getSignalSystemId().toString());
				xmlContainer.getSignalSystem().add(xmlss);
				for (Tuple<Id<SignalGroup>, Id<SignalGroup>> endingBeginningSgIds : intergreens.getEndingBeginningSignalGroupKeys()){
					XMLEndingSignalGroupType xmlEnding = this.getXmlEnding(xmlss.getEndingSignalGroup(), endingBeginningSgIds.getFirst());
					if (xmlEnding == null){
						xmlEnding = fac.createXMLEndingSignalGroupType();
						xmlEnding.setRefId(endingBeginningSgIds.getFirst().toString());
						xmlss.getEndingSignalGroup().add(xmlEnding);
					}
					XMLBeginningSignalGroup xmlBeginn = fac.createXMLEndingSignalGroupTypeXMLBeginningSignalGroup();
					xmlBeginn.setRefId(endingBeginningSgIds.getSecond().toString());
					xmlBeginn.setTimeSeconds(BigInteger.valueOf(intergreens.getIntergreenTime(endingBeginningSgIds.getFirst(), endingBeginningSgIds.getSecond())));
					xmlEnding.getBeginningSignalGroup().add(xmlBeginn);
				}
			}
			return xmlContainer;
		}

		private XMLEndingSignalGroupType getXmlEnding(List<XMLEndingSignalGroupType> endings, Id<SignalGroup> endingId){
			for (XMLEndingSignalGroupType xmlending : endings){
				if (xmlending.getRefId().compareTo(endingId.toString()) == 0){
					return xmlending;
				}
			}
			return null;
		}
		
		public void write(String filename, XMLIntergreenTimes xmlIntergreenTimes) {
			log.info("writing file: " + filename);
	  	JAXBContext jc;
			try {
				jc = JAXBContext.newInstance(org.matsim.jaxb.intergreenTimes10.ObjectFactory.class);
				Marshaller m = jc.createMarshaller();
				super.setMarshallerProperties(IntergreenTimesWriter10.INTERGREENTIMES10, m);
				BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
				m.marshal(xmlIntergreenTimes, bufout);
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
			XMLIntergreenTimes xmlIntergreenTimes = convertDataToXml();
			this.write(filename, xmlIntergreenTimes);
		}

		
	}

