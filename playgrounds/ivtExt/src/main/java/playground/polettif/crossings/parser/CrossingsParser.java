/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.crossings.parser;

import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;


public class CrossingsParser extends MatsimXmlParser {
	
	// ========================================================================
	// static fields
	// ========================================================================
	
	private static final Logger log = Logger.getLogger(CrossingsParser.class);
	
	static final String CROSSINGS_TAG = "crossings";
	
	static final String PTLINK_TAG = "railwayLink";
	static final String PTLINK_KEY_ID = "id";
	
	static final String CROSSING_TAG = "crossing";
	static final String CROSSING_KEY_REFID = "refId";
	static final String CROSSING_KEY_X = "x";
	static final String CROSSING_KEY_Y = "y";

	
	// ========================================================================
	// private members
	// ========================================================================

	private RailLink currentRailLink;
	
	private Map<Id<Link>, RailLink> railwayLinks;

	// ========================================================================
	// constructor
	// ========================================================================

	public CrossingsParser() {
	
	}
	
	// ========================================================================
	// parsing
	// ========================================================================
	
	/**
	 * Parses a file with crossings and returns a list with
	 * instances of {@link RailLink}.
	 *
	 * @param file
	 *            a xml file containing network change events.
	 */
	public Map<Id<Link>, RailLink> parseCrossings(String file) {
		railwayLinks = new HashMap<>();
		super.parse(file);
		return railwayLinks;
	}
	

	@Override
	public void parse(String filename) throws UncheckedIOException {
		railwayLinks = new HashMap<>();
		super.parse(filename);

		log.info("Crossings file read.");

	}

	@Override
	public void parse(URL url) throws UncheckedIOException {
		railwayLinks = new HashMap<>();
		super.parse(url);
	}
	
	// ========================================================================
	// accessor
	// ========================================================================

	/**
	 * Returns the list with public transport links with crossings on them. Be sure to call
	 * {@link #parse(String)}, {@link #parse(String)} or
	 * {@link #parse(URL)} before.
	 * 
	 * @return a list of public transport links, or <tt>null</tt> if
	 *         {@link #parse(String)}, {@link #parse(String)} nor
	 *         {@link #parse(URL)} has been called before.
	 */
	public Map<Id<Link>, RailLink> getRailLinks() {
		return railwayLinks;
	}
	
	// ========================================================================
	// parsing methods
	// ========================================================================

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(PTLINK_TAG)) {
			railwayLinks.put(currentRailLink.getId(), currentRailLink);
			currentRailLink = null;
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		/*
		 * PTLink
		 */
		if(name.equalsIgnoreCase(PTLINK_TAG)) {
			String value = atts.getValue(PTLINK_KEY_ID);
			currentRailLink = new RailLink(value);
		/*
		 * crossingLinks
		 */
		} else if(name.equalsIgnoreCase(CROSSING_TAG) && currentRailLink != null) {
			String refId = atts.getValue(CROSSING_KEY_REFID);
			String x = atts.getValue(CROSSING_KEY_X);
			String y = atts.getValue(CROSSING_KEY_Y);
			if(refId != null) {
				currentRailLink.addCrossing(new Crossing(Id.createLinkId(refId)));
				}
			if(x != null && y != null) {
				currentRailLink.addCrossing(new Crossing(Double.parseDouble(x), Double.parseDouble(y)));
			}
		}
		
	}
	
}
