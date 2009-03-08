/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimSignalSystemsReader
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
import org.matsim.basic.network.BasicLaneDefinitions;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.utils.io.MatsimFileTypeGuesser;
import org.matsim.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

/**
 * Reader for (Light)SignalSystems. The correct parser is 
 * selected automatically if the xml file has a correct header.
 * @author dgrether
 * 
 */
public class MatsimSignalSystemsReader {

	private static final Logger log = Logger
			.getLogger(MatsimSignalSystemsReader.class);

	private static final String SIGNALSYSTEMS10 = "http://www.matsim.org/files/dtd/lightSignalSystems_v1.0.xsd";

	private BasicSignalSystems lightSignalSystems;

	private BasicLaneDefinitions laneDefinitions;
	/**
	 * @deprecated lane definitions have a separate parser.
	 * @param laneDefs
	 * @param signalSystems
	 */
	@Deprecated 
	public MatsimSignalSystemsReader(BasicLaneDefinitions laneDefs, BasicSignalSystems signalSystems) {
		this.laneDefinitions = laneDefs;
		this.lightSignalSystems = signalSystems;
	}

	public void readFile(final String filename) {
		try {
			MatsimFileTypeGuesser fileTypeGuesser = new MatsimFileTypeGuesser(
					filename);
			String sid = fileTypeGuesser.getSystemId();
			
			MatsimJaxbXmlParser reader = null;
			if (sid != null) {
				log.debug("creating parser for system id: " + sid);
				if (sid.compareTo(SIGNALSYSTEMS10) == 0) {
					reader = new LightSignalSystemsReader10(this.laneDefinitions,
							this.lightSignalSystems, sid);
					log.info("Using MatsimLightSignalSystems10Reader ...");
				}
			}
			else {
				String message = "System Id of xml document couldn't be detected. " +
						"Make sure that you try to read a xml document with a valid header. " +
						"If your header seems to be ok, make shure that there is no / at the " +
						"end of the first part of the tuple used as value for xsi:schemaLocation.";
				log.error(message);
				throw new IllegalArgumentException(message);
			}
			log.debug("reading file " + filename);
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
