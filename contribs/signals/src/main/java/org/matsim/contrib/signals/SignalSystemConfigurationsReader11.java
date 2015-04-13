/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemConfigurationsReader11
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
import org.matsim.jaxb.signalsystemsconfig11.XMLSignalSystemConfig;
import org.xml.sax.SAXException;


/**
 * Reader for the signalSystemConfigurations_v1.1.xsd file format
 * @author dgrether
 */
public class SignalSystemConfigurationsReader11 extends MatsimJaxbXmlParser {

	private final static Logger log = Logger.getLogger(SignalSystemConfigurationsReader11.class);

	public SignalSystemConfigurationsReader11(String schemaLocation) {
		super(schemaLocation);
	}
	
	public XMLSignalSystemConfig readSignalSystemConfig11File(String filename) throws SAXException, ParserConfigurationException, IOException, JAXBException{
	  XMLSignalSystemConfig xmlLssConfig;
  	JAXBContext jc;
		jc = JAXBContext.newInstance(org.matsim.jaxb.signalsystemsconfig11.ObjectFactory.class);
		Unmarshaller u = jc.createUnmarshaller();
		//validate file
		super.validateFile(filename, u);
		InputStream stream = null;
		try {
			stream = IOUtils.getInputStream(filename);
			xmlLssConfig = (XMLSignalSystemConfig)u.unmarshal(stream);
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}
		return xmlLssConfig;
	}


	@Override
	public void readFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		throw new UnsupportedOperationException("Use readSignalSystemConfig11File() method");
	}

}
