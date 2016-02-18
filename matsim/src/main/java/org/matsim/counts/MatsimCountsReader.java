/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimCountsReader.java
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

package org.matsim.counts;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for counts-files of MATSim. This reader recognizes the format of the counts-file and uses
 * the correct reader for the specific counts-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimCountsReader extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(MatsimCountsReader.class);
	private final static String COUNTS_V1 = "counts_v1.xsd";

	private final Counts counts;
	private MatsimXmlParser delegate = null;

	private final CoordinateTransformation coordinateTransformation;

	/**
	 * Creates a new reader for MATSim counts files.
	 *
	 * @param counts The Counts-object to store the configuration settings in.
	 */
	public MatsimCountsReader(final Counts counts) {
		this( new IdentityTransformation() , counts );
	}

	/**
	 * Creates a new reader for MATSim counts files.
	 *
	 * @param coordinateTransformation transformation from the CRS of the file to the internal CRS for MATSim
	 * @param counts The Counts-object to store the configuration settings in.
	 */
	public MatsimCountsReader(
			final CoordinateTransformation coordinateTransformation,
			final Counts counts) {
		this.coordinateTransformation = coordinateTransformation;
		this.counts = counts;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	/**
	 * Parses the specified counts file. This method is the same as {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		parse(filename);
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only counts-type is v1
		if (COUNTS_V1.equals(doctype)) {
			this.delegate = new CountsReaderMatsimV1( coordinateTransformation , this.counts);
			log.info("using counts_v1-reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}
