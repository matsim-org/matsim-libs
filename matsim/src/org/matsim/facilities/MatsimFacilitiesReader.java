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

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for facilities-files of MATSim. This reader recognizes the format of the facilities-file and uses
 * the correct reader for the specific facilities-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimFacilitiesReader extends MatsimXmlParser {
	// FIXME: Why is this suddenly a "Matsim"FacilitiesReader and not just a Facilities reader to be consistent with all other 
	// naming conventions?  kai, jan09
	/* because all other readers in Matsim are also called Matsim*Reader,
	 * e.g. MatsimPopulationReader, MatsimNetworkReader, MatsimWorldReader, ...
	 * marcel, feb09
	 */

	private final static String FACILITIES_V1 = "facilities_v1.dtd";

	private final static Logger log = Logger.getLogger(MatsimFacilitiesReader.class);
	
	private final Facilities facilities;
	private MatsimXmlParser delegate = null;

	/**
	 * Creates a new reader for MATSim facilities files.
	 *
	 * @param facilities The Facilities-object to store the facilities in.
	 */
	public MatsimFacilitiesReader(final Facilities facilities) {
		this.facilities = facilities;
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
	 * Parses the specified facilities file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only facilities-type is v1
		if (FACILITIES_V1.equals(doctype)) {
			this.delegate = new FacilitiesReaderMatsimV1(this.facilities);
			log.info("using facilities_v1-reader.");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}
