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

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.AbstractSignalsReader;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.intergreenTimes10.XMLEndingSignalGroupType;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes.XMLSignalSystem;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public final class IntergreenTimesReader10 extends AbstractSignalsReader{
	
	private IntergreenTimesData intergreensData;

	public IntergreenTimesReader10(IntergreenTimesData intergreenTimesData) {
		this.intergreensData = intergreenTimesData;
	}
	
		public void read(InputSource stream) throws UncheckedIOException {
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
