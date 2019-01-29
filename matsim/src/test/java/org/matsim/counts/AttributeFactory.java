/* *********************************************************************** *
 * project: org.matsim.*
 * AttributeFactory.java
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

import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.network.Network;
import org.xml.sax.helpers.AttributesImpl;

public class AttributeFactory {

	public AttributeFactory() {
	}

	public AttributesImpl createCountsAttributes() {
		AttributesImpl meta=new AttributesImpl();

		//String uri, String localName, String qName, String type, String value
		meta.addAttribute("", "", "name", "xsd:string", "testName");
		meta.addAttribute("", "", "desc", "xsd:string", "testDesc");
		meta.addAttribute("", "", "year", "xsd:gYear", "2000");
		meta.addAttribute("", "", "layer", "xsd:string", "testLayer");

		return meta;
	}

	public AttributesImpl createCountAttributes() {
		AttributesImpl meta=new AttributesImpl();

		//String uri, String localName, String qName, String type, String value
		meta.addAttribute("", "", "loc_id", "xsd:nonNegativeInteger", "1");
		meta.addAttribute("", "", "cs_id", "xsd:string", "testNr");

		return meta;
	}

	public AttributesImpl createCountAttributesWithCoords() {
		AttributesImpl meta=createCountAttributes();

		//String uri, String localName, String qName, String type, String value
		meta.addAttribute("", "", "x", "xsd:double", "123.456");
		meta.addAttribute("", "", "y", "xsd:double", "987.654");

		return meta;
	}

	public AttributesImpl createVolumeAttributes() {
		AttributesImpl meta=new AttributesImpl();

		//String uri, String localName, String qName, String type, String value
		meta.addAttribute("", "", "h", "xsd:nonNegativeInteger", "1");
		meta.addAttribute("", "", "val", "xsd:string", "100.0");

		return meta;
	}

	public CalcLinkStats createLinkStats(Network network) {
		CalcLinkStats linkStats = new CalcLinkStats(network);
		linkStats.readFile("./test/input/org/matsim/counts/linkstats.att");
		return linkStats;
	}

}