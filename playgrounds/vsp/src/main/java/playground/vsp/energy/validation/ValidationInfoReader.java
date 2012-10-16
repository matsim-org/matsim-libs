/* *********************************************************************** *
 * project: org.matsim.*
 * ValidationInfoReader
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

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * @author dgrether
 *
 */
public class ValidationInfoReader {

	public ValidationInformation readFile(String filename){
		ValidationInformation list = null;
		try {
			JAXBContext ctx;
			ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Unmarshaller unmarshaller = ctx.createUnmarshaller();
			list= (ValidationInformation) unmarshaller.unmarshal(new File(filename));
		} catch (JAXBException e) {
			e.printStackTrace();
		}
    return list;
	}
	
}
