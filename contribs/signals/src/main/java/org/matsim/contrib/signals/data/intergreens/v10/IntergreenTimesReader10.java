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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.intergreenTimes10.XMLEndingSignalGroupType;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes.XMLSignalSystem;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class IntergreenTimesReader10 implements MatsimReader {

	private static final Logger log = Logger.getLogger(IntergreenTimesReader10.class);
	
	private IntergreenTimesData intergreensData;

	public IntergreenTimesReader10(IntergreenTimesData intergreenTimesData) {
		this.intergreensData = intergreenTimesData;
	}
	
	
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		log.info("starting unmarshalling " + filename);
		try (InputStream stream = IOUtils.getInputStream(filename)){
			readStream(stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void readStream(InputStream stream) throws UncheckedIOException {
		try {
			Unmarshaller u = JAXBContext.newInstance(org.matsim.jaxb.intergreenTimes10.ObjectFactory.class).createUnmarshaller();
			u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/intergreenTimes_v1.0.xsd")));
			XMLIntergreenTimes xmlIntergreenTimes = (XMLIntergreenTimes) u.unmarshal(stream);
			IntergreenTimesDataFactory factory = this.intergreensData.getFactory();
			for (XMLSignalSystem xmlSignalSystem : xmlIntergreenTimes.getSignalSystem()){
				Id<SignalSystem> signalSystemId = Id.create(xmlSignalSystem.getRefId(), SignalSystem.class);
				IntergreensForSignalSystemData intergreens = factory.createIntergreensForSignalSystem(signalSystemId);
				this.intergreensData.addIntergreensForSignalSystem(intergreens);
				for (XMLEndingSignalGroupType xmlEndingSg : xmlSignalSystem.getEndingSignalGroup()){
					for (XMLEndingSignalGroupType.XMLBeginningSignalGroup xmlBeginningSg : xmlEndingSg.getBeginningSignalGroup()) {
						Id<SignalGroup> endingId = Id.create(xmlEndingSg.getRefId(), SignalGroup.class);
						Id<SignalGroup> beginningId = Id.create(xmlBeginningSg.getRefId(), SignalGroup.class);
						intergreens.setIntergreenTime(xmlBeginningSg.getTimeSeconds().intValue(), endingId, beginningId);
					}
				}
			}
		} catch (JAXBException | SAXException e) {
			throw new UncheckedIOException(e);
		}
	}
}
