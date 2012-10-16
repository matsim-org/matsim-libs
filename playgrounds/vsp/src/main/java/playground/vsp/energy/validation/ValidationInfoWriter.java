/* *********************************************************************** *
 * project: org.matsim.*
 * ValidationListWriter
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
package playground.vsp.energy.validation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

/**
 * @author dgrether
 *
 */
public class ValidationInfoWriter {
	
	private static final Logger log = Logger.getLogger(ValidationInfoWriter.class);
	
	private ValidationInformation validationInformation;

	public ValidationInfoWriter(ValidationInformation info){
		validationInformation = info;
	}
	
	public void writeFile(String filename){
		JAXBContext ctx;
		RuntimeException ex = null;
		try {
			ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Marshaller marshaller = ctx.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
			marshaller.marshal(this.validationInformation, new FileOutputStream(filename));
		} catch (JAXBException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		}
		finally {
			if (ex != null){
				throw ex;
			} else {
				log.info("file written to " + filename);
			}
		}

	}

}
