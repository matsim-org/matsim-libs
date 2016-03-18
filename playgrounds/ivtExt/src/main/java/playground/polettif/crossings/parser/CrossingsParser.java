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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;


public class CrossingsParser extends MatsimXmlParser {
	
	// ========================================================================
	// static fields
	// ========================================================================
	
	private static final Logger log = Logger.getLogger(CrossingsParser.class);
	
	static final String CROSSINGS_TAG = "crossings";
	
	static final String PTLINK_TAG = "ptLink";
	static final String PTLINK_ID_TAG = "id";
	
	static final String CROSSINGLINK_TAG = "crossingLink";
	static final String CROSSINGLINK_ID_TAG = "refId";

	
	// ========================================================================
	// private members
	// ========================================================================

	private PTLink currentPTLink;
	
	private List<PTLink> ptLinks;
	
	private Map<Id<Link>, List<String>> crossings = new HashMap<>();
	
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
	 * instances of {@link PTLink}.
	 * 
	 * @param file
	 *            a xml file containing network change events.
	 */
	public List<PTLink> parsePTLinks(String file) {
		ptLinks = new ArrayList<>();
		super.parse(file);
		return ptLinks;
	}
	

	@Override
	public void parse(String filename) throws UncheckedIOException {
		ptLinks = new ArrayList<>();
		super.parse(filename);
			
		for (PTLink currentPTLink : ptLinks) {
			this.crossings.put(currentPTLink.getId(), currentPTLink.getCrossingLinks());
		}
		
		log.info("Crossings file read.");

	}

	@Override
	public void parse(URL url) throws UncheckedIOException {
		ptLinks = new ArrayList<>();
		super.parse(url);
	}
	
	// ========================================================================
	// accessor
	// ========================================================================

	/**
	 * Returns the list with public transport links with crossings on them. Be sure to call
	 * {@link #parsePTLinks(String)}, {@link #parse(String)} or
	 * {@link #parse(URL)} before.
	 * 
	 * @return a list of public transport links, or <tt>null</tt> if
	 *         {@link #parsePTLinks(String)}, {@link #parse(String)} nor
	 *         {@link #parse(URL)} has been called before.
	 */
	public List<PTLink> getPTLinks() {
		return ptLinks;
	}
	
	public Map<Id<Link>, List<String>> getCrossings() {		
		return crossings;
	}
	
	// ========================================================================
	// parsing methods
	// ========================================================================

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(PTLINK_TAG)) {
			ptLinks.add(currentPTLink);
			currentPTLink = null;
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		/*
		 * PTLink
		 */
		if(name.equalsIgnoreCase(PTLINK_TAG)) {
			String value = atts.getValue(PTLINK_ID_TAG);
			currentPTLink = new PTLink(value);
		/*
		 * crossingLinks
		 */
		} else if(name.equalsIgnoreCase(CROSSINGLINK_TAG) && currentPTLink != null) {
			String value = atts.getValue(CROSSINGLINK_ID_TAG);
			if(value != null) {
				currentPTLink.addCrossingLink(value);
				}
		}
		
	}
	
}
