/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimJaxbXmlWriter
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
package org.matsim.core.utils.io;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;



/**
 * This abstract class serves as an marker interface for the
 * method write file and provides methods to be called by
 * each MATSim Jaxb based xml writer before marshalling
 * the java instances to xml.
 * 
 * @author dgrether
 *
 */
public abstract class MatsimJaxbXmlWriter {

	public void setMarshallerProperties(String schemaLocation, Marshaller m) throws PropertyException  {
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, 
				Boolean.TRUE); 
		m.setProperty("jaxb.schemaLocation", MatsimXmlWriter.MATSIM_NAMESPACE + " " + schemaLocation);
	}

	
	public abstract void writeFile(String filename);
	
}
