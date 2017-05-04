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

package playground.nmviljoen.io;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.nmviljoen.gridExperiments.GridExperiment;

public class MultilayerInstanceReader extends MatsimXmlParser {
	private final static String MULTILAYER_NETWORK_V1 = "multilayerNetwork_v1.dtd";
	private final static Logger LOG = Logger.getLogger(MultilayerInstanceReader.class);
	private MatsimXmlParser delegate = null;
	private GridExperiment experiment;
	
	/**
	 * Creates a new reader for Digicore vehicle files.
	 */
	public MultilayerInstanceReader(GridExperiment experiment) {
		this.experiment = experiment;
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
		if (MULTILAYER_NETWORK_V1.equals(doctype)) {
			this.delegate = new MultilayerInstanceReader_v1(this.experiment);
			LOG.info("Using multilayerNetwork_v1 reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}

