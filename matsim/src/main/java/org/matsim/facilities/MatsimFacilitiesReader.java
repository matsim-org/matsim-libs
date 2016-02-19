/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFacilitiesReader.java
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

package org.matsim.facilities;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;

/**
 * A reader for facilities-files of MATSim. This reader recognizes the format of the facilities-file and uses
 * the correct reader for the specific facilities-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimFacilitiesReader extends MatsimXmlParser implements MatsimSomeReader {
	/* Why is this suddenly a "Matsim"FacilitiesReader and not just a Facilities reader to be consistent with all other 
	 * naming conventions?  kai, jan09
	 * because all other readers in Matsim are also called Matsim*Reader,
	 * e.g. MatsimPopulationReader, MatsimNetworkReader, MatsimWorldReader, ...
	 * marcel, feb09
	 * The logic seems to be:
	 * - there is a "basic" MatsimXmlParser
	 * - there are implementations AbcReaderMatsimVx
	 * - there is a meta-class MatsimReaderAbc, which calls the Vx-Readers depending on the version
	 * - yy there is usually also an interface AbcReader, which is, however, not consistent:
	 *   () sometimes, it is there, and sometimes not
	 *   () sometimes, it is read(), sometimes it is readFile( file), sometimes ...
	 *   () sometimes it throws an i/o exception, sometimes not
	 * Oh well.  
	 * At least it seems indeed that the MatsimReader is indeed usually there. kai, jul09
	 */


	private final static String FACILITIES_V1 = "facilities_v1.dtd";

	private final static Logger log = Logger.getLogger(MatsimFacilitiesReader.class);

	private final CoordinateTransformation coordinateTransformation;
	private final Scenario scenario;
	private MatsimXmlParser delegate = null;

	/**
	 * Creates a new reader for MATSim facilities files.
	 *
	 * @param scenario The scenario containing the Facilities-object to store the facilities in.
	 */
	public MatsimFacilitiesReader(final Scenario scenario) {
		this( new IdentityTransformation() , scenario );
	}

	/**
	 * Creates a new reader for MATSim facilities files.
	 *
	 * @param coordinateTransformation a transformation from the CRS of the data file to the CRS inside MATSim
	 * @param scenario The scenario containing the Facilities-object to store the facilities in.
	 */
	public MatsimFacilitiesReader(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		this.coordinateTransformation = coordinateTransformation;
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

	/**
	 * Parses the specified facilities file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only facilities-type is v1
		if (FACILITIES_V1.equals(doctype)) {
			this.delegate = new FacilitiesReaderMatsimV1( coordinateTransformation , scenario );
			log.info("using facilities_v1-reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}
