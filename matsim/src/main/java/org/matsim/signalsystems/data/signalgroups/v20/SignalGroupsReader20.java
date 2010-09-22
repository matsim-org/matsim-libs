/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsReader20
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.data.signalgroups.v20;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;



/**
 * @author dgrether
 *
 */
public class SignalGroupsReader20 extends MatsimJaxbXmlParser {

	private SignalGroupsData signalGroupsData;
	private SignalGroupsDataFactory factory;

	public SignalGroupsReader20(SignalGroupsData signalGroupsData, String schemaLocation) {
		super(schemaLocation);
		this.signalGroupsData = signalGroupsData;
		this.factory = this.signalGroupsData.getFactory();
	}

	@Override
	public void readFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		
	}
	
}
