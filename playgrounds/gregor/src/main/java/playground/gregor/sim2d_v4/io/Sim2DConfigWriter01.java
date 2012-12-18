/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfigSerializer.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.gregor.sim2d_v4.io.jaxb.sim2config01.ObjectFactory;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DConfigType;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DEnvironmentType;
import playground.gregor.sim2d_v4.io.jaxb.sim2config01.XMLSim2DEnvironmentsType;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;

public class Sim2DConfigWriter01 extends MatsimJaxbXmlWriter {
	
	private static final Logger log = Logger.getLogger(Sim2DConfigWriter01.class);

//	public static final String SCHEMA = "http://matsim.org/files/dtd/sim2DConfig_v0.1.xsd";
	public static final String SCHEMA = "http://svn.vsp.tu-berlin.de/repos/public-svn/xml-schemas/sim2DConfig_v0.1.xsd";
	private final Sim2DConfig conf;

	public Sim2DConfigWriter01(Sim2DConfig conf) {
		this.conf = conf;
	}
	
	@Override
	public void write(String filename) {
		JAXBElement<XMLSim2DConfigType> jconf = createXMLSim2DConfigType();
		log.info("writing file:" + filename);
		try {
			JAXBContext jc = JAXBContext.newInstance(playground.gregor.sim2d_v4.io.jaxb.sim2config01.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(SCHEMA, m);
			BufferedWriter buffout = IOUtils.getBufferedWriter(filename);
			m.marshal(jconf, buffout);
			buffout.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private JAXBElement<XMLSim2DConfigType> createXMLSim2DConfigType() {
		ObjectFactory fac = new ObjectFactory();
		
		XMLSim2DConfigType xmlConfType = fac.createXMLSim2DConfigType();
		xmlConfType.setEventsInterval(this.conf.getEventsInterval());
		xmlConfType.setTimeStepSize(this.conf.getTimeStepSize());
		XMLSim2DEnvironmentsType xmlEnvs = fac.createXMLSim2DEnvironmentsType();
		xmlConfType.setSim2DEnvironments(xmlEnvs);
		
		for (String env : this.conf.getSim2DEnvironmentPaths()){
			XMLSim2DEnvironmentType xmlEnv = fac.createXMLSim2DEnvironmentType();
			xmlEnvs.getSim2DEnvironment().add(xmlEnv);
			xmlEnv.setNetworkFilePath(this.conf.getNetworkPath(env));
			xmlEnv.setSim2DEnvironmentPath(env);
		}
		
		JAXBElement<XMLSim2DConfigType> ret = fac.createSim2DConfig(xmlConfType );
		return ret;
	}


}
