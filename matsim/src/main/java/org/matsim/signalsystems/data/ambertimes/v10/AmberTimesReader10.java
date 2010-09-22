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

package org.matsim.signalsystems.data.ambertimes.v10;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem;
import org.matsim.jaxb.amberTimes10.XMLAmberTimes.XMLSignalSystem.XMLSignal;
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

	@Override
	public void readFile(final String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		// create jaxb infrastructure
		JAXBContext jc;
		XMLAmberTimes xmlatdefs = null;

		jc = JAXBContext.newInstance(org.matsim.jaxb.amberTimes10.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		log.info("starting to validate " + filename);
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
		InputStream stream = null;
		try {
			stream = IOUtils.getInputstream(filename);
			xmlatdefs = (XMLAmberTimes) u.unmarshal(stream);
		} catch (Exception ex) {
			ex.printStackTrace();
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
		amberTimesData
				.setDefaultAmber(xmlatdefs.getGlobalDefaults().getAmber().getSeconds().intValue());
		amberTimesData.setDefaultRedAmber(xmlatdefs.getGlobalDefaults().getRedAmber().getSeconds()
				.intValue());
		amberTimesData.setDefaultAmberTimeGreen(xmlatdefs.getGlobalDefaults().getAmberTimeGreen()
				.getProportion().doubleValue());

		for (XMLSignalSystem xmlss : xmlatdefs.getSignalSystem()) {
			Id ssid = new IdImpl(xmlss.getRefId().toString());
			AmberTimeData atdata = new AmberTimeDataImpl(ssid);

			// Signalsystem Defaults
			atdata.setDefaultAmber(xmlss.getSystemDefaults().getAmber().getSeconds().intValue());
			atdata.setDefaultRedAmber(xmlss.getSystemDefaults().getRedAmber().getSeconds().intValue());

			for (XMLSignal xmls : xmlss.getSignal()) {

				Id sid = new IdImpl(xmls.getRefId());
				atdata.setAmberTimeOfSignal(sid, xmls.getAmber().getSeconds().intValue());
				atdata.setRedAmberTimeOfSignal(sid, xmls.getRedAmber().getSeconds().intValue());

			}

			amberTimesData.addAmberTimeData(atdata);
		}

	}

}
