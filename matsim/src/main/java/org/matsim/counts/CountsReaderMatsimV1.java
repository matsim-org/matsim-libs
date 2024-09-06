/* *********************************************************************** *
 * project: org.matsim.*
 * CountsReaderMatsimV1.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * A reader for counts-files of MATSim according to <code>counts_v1.xsd</code>.
 *
 * @author mrieser
 */
public class CountsReaderMatsimV1 extends MatsimXmlParser {

	private final static String COUNTS = "counts";
	private final static String COUNT = "count";
	private final static String VOLUME = "volume";

	private final CoordinateTransformation coordinateTransformation;

	private final Counts counts;
	private Count currcount = null;

	private static final Logger log = LogManager.getLogger(CountsReaderMatsimV1.class);

	public CountsReaderMatsimV1(final Counts counts) {
		this( new IdentityTransformation(), counts );
	}

	public CountsReaderMatsimV1(
			final CoordinateTransformation coordinateTransformation,
			final Counts counts) {
		super(ValidationType.XSD_ONLY);
		this.coordinateTransformation = coordinateTransformation;
		this.counts = counts;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (VOLUME.equals(name)) {
			startVolume(atts);
		} else if (COUNT.equals(name)) {
			startCount(atts);
		} else if (COUNTS.equals(name)) {
			startCounts(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {

	}

	private void startCounts(final Attributes meta) {
		this.counts.setName(meta.getValue("name"));
		this.counts.setDescription(meta.getValue("desc"));
		this.counts.setYear(Integer.parseInt(meta.getValue("year")));
	}

	private void startCount(final Attributes meta) {
		String locId = meta.getValue("loc_id");
		this.currcount = this.counts.createAndAddCount(Id.create(locId, Link.class), meta.getValue("cs_id"));
		String x = meta.getValue("x");
		String y = meta.getValue("y");
		if (x != null && y != null) {
			this.currcount.setCoord(
					coordinateTransformation.transform(
							new Coord(
									Double.parseDouble(x),
									Double.parseDouble(y))));
		}
	}

	private void startVolume(final Attributes meta) {
		if (this.currcount != null) {
			this.currcount.createVolume(Integer.parseInt(meta.getValue("h")), Double.parseDouble(meta.getValue("val")));
		}
	}

}
