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

package playground.polettif.crossings;

import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;
import playground.polettif.crossings.lib.CrossingImpl;
import playground.polettif.crossings.lib.PtLink;
import playground.polettif.crossings.lib.PtLinkImpl;

/**
 * Used to parse a crossings file.
 */
public class CrossingsFileParser extends MatsimXmlParser {
	
	// ========================================================================
	// static fields
	// ========================================================================
	
	private static final Logger log = Logger.getLogger(CrossingsFileParser.class);
	
	static final String CROSSINGS_TAG = "crossings";
	
	static final String PTLINK_TAG = "ptLink";
	static final String PTLINK_KEY_ID = "id";
	
	static final String CROSSING_TAG = "crossing";
	static final String CROSSING_KEY_REFID = "refId";

	// todo coordinates are currently not used
	static final String CROSSING_KEY_X = "x";
	static final String CROSSING_KEY_Y = "y";

	
	// ========================================================================
	// private members
	// ========================================================================

	private PtLink currentRailLink;
	
	private final Map<Id<Link>, PtLink> railwayLinks;

	// ========================================================================
	// constructor
	// ========================================================================

	public CrossingsFileParser(Map<Id<Link>,PtLink> railwayLinks ) {
		this.railwayLinks = railwayLinks ;
	}
	
	// ========================================================================
	// parsing
	// ========================================================================
	
//	/**
//	 * Parses a file with crossings and returns a list with
//	 * instances of {@link PtLink}.
//	 *
//	 * @param file
//	 *            a xml file containing network change events.
//	 */
//	public Map<Id<Link>, PtLink> parseCrossings(String file) {
//		super.parse(file);
//		return railwayLinks;
//	}
//	
//
//	@Override
//	public void parse(String filename) throws UncheckedIOException {
//		railwayLinks = new HashMap<>();
//		super.parse(filename);
//
//		log.info("Crossings file read.");
//
//	}
//
//	@Override
//	public void parse(URL url) throws UncheckedIOException {
//		railwayLinks = new HashMap<>();
//		super.parse(url);
//	}
	
	// ========================================================================
	// accessor
	// ========================================================================
//
//	/**
//	 * Returns the list with public transport links with crossings on them. Be sure to call
//	 * {@link #parse(String)}, {@link #parse(String)} or
//	 * {@link #parse(URL)} before.
//	 * 
//	 * @return a list of public transport links, or <tt>null</tt> if
//	 *         {@link #parse(String)}, {@link #parse(String)} nor
//	 *         {@link #parse(URL)} has been called before.
//	 */
//	public Map<Id<Link>, PtLink> getPtLinks() {
//		return railwayLinks;
//	}
	
	// ========================================================================
	// parsing methods
	// ========================================================================

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(PTLINK_TAG)) {
			railwayLinks.put(currentRailLink.getLinkId(), currentRailLink);
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
			currentRailLink = new PtLinkImpl(value);
		/*
		 * crossingLinks
		 */
		} else if(name.equalsIgnoreCase(CROSSING_TAG) && currentRailLink != null) {
			String refId = atts.getValue(CROSSING_KEY_REFID);
			String x = atts.getValue(CROSSING_KEY_X);
			String y = atts.getValue(CROSSING_KEY_Y);
			if(refId != null) {
				currentRailLink.addCrossing(new CrossingImpl(Id.createLinkId(refId)));
				}
			if(x != null && y != null) {
				currentRailLink.addCrossing(new CrossingImpl(Double.parseDouble(x), Double.parseDouble(y)));
			}
		}
		
	}
	
}
