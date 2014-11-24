/* *********************************************************************** *
 * project: org.matsim.*
 * GexfWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.minibus.genericUtils.gexf;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedWriter;
import java.io.IOException;



/**
 * @author dgrether
 *
 */
class GexfWriter extends MatsimJaxbXmlWriter {

	private static final Logger log = Logger.getLogger(GexfWriter.class);
	
	private final static String xsdPath = "http://www.gexf.net/1.2draft/gexf.xsd";

	private final XMLGexfContent gexfContent;

	private GexfWriter(XMLGexfContent gexf){
		this.gexfContent = gexf;
	}
	
	@Override
	public void write(String filename) {
		log.info("writing output to " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.contrib.minibus.genericUtils.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(xsdPath, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(gexfContent, bufout);
			bufout.close();
			log.info(filename + " written successfully.");
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

}
