/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimMatricesReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.matrices;

import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for matrices-files of MATSim. This reader recognizes the format of the matrices-file and uses
 * the correct reader for the specific version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimMatricesReader extends MatsimXmlParser {

	private final static Logger log = LogManager.getLogger(MatsimMatricesReader.class);

	private final static String MATRICES_V1 = "matrices_v1.dtd";

	private final Matrices matrices;
	private MatsimXmlParser delegate = null;
	private final Scenario scenario;

	/**
	 * Creates a new reader for MATSim matrices files.
	 *
	 * @param matrices The Matrices-object to store the data in.
	 * @param scenario The scenario containing the world/layers the matrices reference to.
	 */
	public MatsimMatricesReader(final Matrices matrices, final Scenario scenario) {
		super(ValidationType.DTD_ONLY);
		this.matrices = matrices;
		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only matrices-type is v1
		if (MATRICES_V1.equals(doctype)) {
			this.delegate = new MatricesReaderMatsimV1(this.matrices);
			log.info("using matrices_v1-reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}
