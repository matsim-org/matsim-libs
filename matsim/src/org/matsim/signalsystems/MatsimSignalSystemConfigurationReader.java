/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimSignalSystemConfigurationReader
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

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.utils.io.MatsimFileTypeGuesser;
import org.matsim.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;


/**
 * Reader for (Light)SignalSystemConfigurations and the corresponding
 * file format (light)signalSystemConfigurations_v1.*.xsd. The correct parser is 
 * selected automatically if the xml file has a correct header.
 * @author dgrether
 *
 */
public class MatsimSignalSystemConfigurationReader {

	private static final Logger log = Logger
			.getLogger(MatsimSignalSystemConfigurationReader.class);

	private static final String SIGNALSYSTEMSCONFIG10 = "lightSignalSystemsConfig_v1.0.xsd";

	private BasicSignalSystemConfigurations lightSignalSystemConfigs;


	public MatsimSignalSystemConfigurationReader(BasicSignalSystemConfigurations lssConfigs) {
		this.lightSignalSystemConfigs = lssConfigs;
	}

	public void readFile(final String filename) {
		try {
			MatsimFileTypeGuesser fileTypeGuesser = new MatsimFileTypeGuesser(
					filename);
			String sid = fileTypeGuesser.getSystemId();
			MatsimJaxbXmlParser reader = null;
			if (sid != null) {
				if (sid.endsWith(SIGNALSYSTEMSCONFIG10)) {
					
					reader = new LightSignalSystemConfigurationsReader10(this.lightSignalSystemConfigs, sid);
					log.info("Using LightSignalSystemConfigurations10Reader ...");
				}
			}
			else {
				throw new IllegalArgumentException(
						"System Id of xml document couldn't be detected. Make shure that you try to read a xml document with a valid header.");
			}
			reader.readFile(filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
