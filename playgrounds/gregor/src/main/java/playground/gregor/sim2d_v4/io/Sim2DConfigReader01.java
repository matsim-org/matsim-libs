/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfigReader01.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.io;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.SAXException;

import playground.gregor.sim2d_v4.io.jaxb.sim2config01.ObjectFactory;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DConfigType;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DEnvironmentType;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DEnvironmentsType;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;

public class Sim2DConfigReader01 extends MatsimJaxbXmlParser {

	private static final Logger log = Logger.getLogger(Sim2DConfigReader01.class);

	public static final String SCHEMA = "http://svn.vsp.tu-berlin.de/repos/public-svn/xml-schemas/sim2DConfig_v0.1.xsd";

	private final Sim2DConfig config;
	private final boolean isValidating;
	public Sim2DConfigReader01(Sim2DConfig config, String schemaLocation, boolean isValidating){
		super(schemaLocation);
		this.config = config;
		this.isValidating = isValidating;
	}

	public Sim2DConfigReader01(Sim2DConfig config, boolean isValidating) {
		this(config,SCHEMA,isValidating);
	}

	@Override
	public void readFile(String filename) {
		JAXBContext jc;
		JAXBElement<XMLSim2DConfigType> jxmlConf = null;
		InputStream stream = null;

		try {
			jc = JAXBContext.newInstance(ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			if (this.isValidating){
				validate(filename,u);
			}
			log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			jxmlConf = (JAXBElement<XMLSim2DConfigType>) u.unmarshal(stream);
		} catch(JAXBException e) {
			throw new UncheckedIOException(e);
		}

		
		//convert jaxb to matsim
		XMLSim2DConfigType xmlConf = jxmlConf.getValue();
		this.config.setEventsInterval(xmlConf.getEventsInterval());
		this.config.setTimeStepSize(xmlConf.getTimeStepSize());
		XMLSim2DEnvironmentsType xmlenvs = xmlConf.getSim2DEnvironments();
		for (XMLSim2DEnvironmentType xmlenv : xmlenvs.getSim2DEnvironment()) {
			String envPath = xmlenv.getSim2DEnvironmentPath();
			String netPAth = xmlenv.getNetworkFilePath();
			
			this.config.addSim2DEnvironmentPath(envPath);
			this.config.addSim2DEnvNetworkMapping(envPath, netPAth);
			
		}
	}


	private void validate(String filename, Unmarshaller u) {
		//validate xml file
		log.info("starting to validate " + filename);
		try {
			super.validateFile(filename, u);
		} catch (SAXException e) {
			throw new UncheckedIOException(e);
		} catch (ParserConfigurationException e) {
			throw new UncheckedIOException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

}
