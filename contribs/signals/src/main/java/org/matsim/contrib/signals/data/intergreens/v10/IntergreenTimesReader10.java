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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.jaxb.intergreenTimes10.XMLEndingSignalGroupType;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes;
import org.matsim.jaxb.intergreenTimes10.XMLIntergreenTimes.XMLSignalSystem;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesDataFactory;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreensForSignalSystemData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class IntergreenTimesReader10 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(IntergreenTimesReader10.class);
	
	private IntergreenTimesData intergreensData;

	public IntergreenTimesReader10(IntergreenTimesData intergreenTimesData, String schemaLocation) {
		super(schemaLocation);
		this.intergreensData = intergreenTimesData;
	}

	public IntergreenTimesReader10(IntergreenTimesData intergreenTimesData) {
		this(intergreenTimesData, IntergreenTimesWriter10.INTERGREENTIMES10);
	}
	
	
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLIntergreenTimes xmlIntergreenTimes = null;
		InputStream stream = null;
		try {

		jc = JAXBContext.newInstance(org.matsim.jaxb.intergreenTimes10.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		log.info("starting to validate " + filename);
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			xmlIntergreenTimes = (XMLIntergreenTimes) u.unmarshal(stream);
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
			
		} catch (JAXBException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UncheckedIOException(e);
		} catch (ParserConfigurationException e) {
			throw new UncheckedIOException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);			
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}
	}

}
