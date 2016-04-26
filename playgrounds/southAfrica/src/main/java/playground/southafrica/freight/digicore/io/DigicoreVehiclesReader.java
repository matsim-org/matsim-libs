/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleReader.java
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

package playground.southafrica.freight.digicore.io;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public class DigicoreVehiclesReader extends MatsimXmlParser {
	private final static String DIGICORE_VEHICLES_V1 = "digicoreVehicles_v1.dtd";
	private final static Logger LOG = Logger.getLogger(DigicoreVehiclesReader.class);
	private MatsimXmlParser delegate = null;
	private DigicoreVehicles vehicles;

	
	/**
	 * Creates a new reader for Digicore vehicle files.
	 */
	public DigicoreVehiclesReader(DigicoreVehicles vehicles) {
		this.vehicles = vehicles;
	}
	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}
	
	
	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only digicoreVehicles-type is v1
		if (DIGICORE_VEHICLES_V1.equals(doctype)) {
			this.delegate = new DigicoreVehiclesReader_v1(this.vehicles);
			LOG.info("using digicoreVehicle_v1 reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}

