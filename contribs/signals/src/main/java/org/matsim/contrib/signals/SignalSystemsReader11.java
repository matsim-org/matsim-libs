/* *********************************************************************** *
 * project: org.matsim.*
 * LightSignalSystemsReader11
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
package org.matsim.contrib.signals;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.jaxb.signalsystems11.XMLSignalSystems;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class SignalSystemsReader11 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(SignalSystemsReader11.class);


	public SignalSystemsReader11(String schemaLocation) {
		super(schemaLocation);
	}
	
	public XMLSignalSystems readSignalSystems11File(String filename) throws JAXBException, SAXException, ParserConfigurationException, IOException{
		// create jaxb infrastructure
		JAXBContext jc;
		XMLSignalSystems xmlLssDefinition;
		jc = JAXBContext
				.newInstance(org.matsim.jaxb.signalsystems11.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		// validate XML file
		super.validateFile(filename, u);
		log.info("starting unmarshalling " + filename);
		InputStream stream = null;
		try {
//			stream = new FileInputStream(filename);
		  stream = IOUtils.getInputStream(filename);
			xmlLssDefinition = (XMLSignalSystems) u.unmarshal(stream);
			log.info("unmarshalling complete");
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}
		return xmlLssDefinition;
	}
	

	@Override
	public void readFile(final String filename) throws JAXBException,
			SAXException, ParserConfigurationException, IOException {
		throw new UnsupportedOperationException("Use readSignalSystems11File() method");
	}
}
