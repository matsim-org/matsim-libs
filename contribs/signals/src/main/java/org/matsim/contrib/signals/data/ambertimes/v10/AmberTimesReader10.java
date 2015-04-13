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
import org.matsim.jaxb.amberTimes10.XMLAmberTimes;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem.XMLSignal;
import org.matsim.signals.data.ambertimes.v10.AmberTimeData;
import org.matsim.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalSystem;
import org.xml.sax.SAXException;

/**
 * @author jbischoff
 * @author dgrether
 */
public class AmberTimesReader10 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(AmberTimesReader10.class);
	private AmberTimesData amberTimesData;

	public AmberTimesReader10(AmberTimesData amberTimesData, String schemaLocation) {
		super(schemaLocation);
		this.amberTimesData = amberTimesData;
	}

	public AmberTimesReader10(AmberTimesData amberTimesData) {
		this(amberTimesData, AmberTimesWriter10.AMBERTIMES10);
	}
	
	
	@Override
	public void readFile(final String filename) {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLAmberTimes xmlatdefs = null;
		InputStream stream = null;
		try {
			jc = JAXBContext.newInstance(org.matsim.jaxb.amberTimes10.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			// validate XML file
			log.info("starting to validate " + filename);
			super.validateFile(filename, u);
			log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			xmlatdefs = (XMLAmberTimes) u.unmarshal(stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (JAXBException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UncheckedIOException(e);
		} catch (ParserConfigurationException e) {
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

		// convert from Jaxb types to MATSim-API conform types

		// Global Defaults
		if (xmlatdefs.getGlobalDefaults() != null){
			if (xmlatdefs.getGlobalDefaults().getAmber() != null){
				amberTimesData
				.setDefaultAmber(xmlatdefs.getGlobalDefaults().getAmber().getSeconds().intValue());
			}
			if (xmlatdefs.getGlobalDefaults().getAmberTimeGreen() != null){
				amberTimesData.setDefaultAmberTimeGreen(xmlatdefs.getGlobalDefaults().getAmberTimeGreen()
						.getProportion().doubleValue());
			}
			if (xmlatdefs.getGlobalDefaults().getRedAmber() != null){
				amberTimesData.setDefaultRedAmber(xmlatdefs.getGlobalDefaults().getRedAmber().getSeconds()
						.intValue());
			}
		}

		for (XMLSignalSystem xmlss : xmlatdefs.getSignalSystem()) {
			Id<SignalSystem> ssid = Id.create(xmlss.getRefId(), SignalSystem.class);
			AmberTimeData atdata = new AmberTimeDataImpl(ssid);

			// Signalsystem Defaults
			if (xmlss.getSystemDefaults() != null){
				if (xmlss.getSystemDefaults().getAmber() != null){
					atdata.setDefaultAmber(xmlss.getSystemDefaults().getAmber().getSeconds().intValue());
				}
				if (xmlss.getSystemDefaults().getRedAmber() != null){
					atdata.setDefaultRedAmber(xmlss.getSystemDefaults().getRedAmber().getSeconds().intValue());
				}
			}

			for (XMLSignal xmls : xmlss.getSignal()) {

				Id<Signal> sid = Id.create(xmls.getRefId(), Signal.class);
				atdata.setAmberTimeOfSignal(sid, xmls.getAmber().getSeconds().intValue());
				atdata.setRedAmberTimeOfSignal(sid, xmls.getRedAmber().getSeconds().intValue());

			}

			amberTimesData.addAmberTimeData(atdata);
		}

	}

}
