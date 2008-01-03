/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingReaderXMLv1.java
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

package org.matsim.roadpricing;

import java.util.Stack;

import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;

public class RoadPricingReaderXMLv1 extends MatsimXmlParser  {

	private final static String TAG_ROADPRICING = "roadpricing";
	private final static String TAG_DESCRIPTION = "description";
	private final static String TAG_LINK = "link";
	private final static String TAG_COST = "cost";

	private final static String ATTR_NAME = "name";
	private final static String ATTR_TYPE = "type";
	private final static String ATTR_ACTIVE = "active";
	private final static String ATTR_ID = "id";
	private final static String ATTR_START_TIME = "start_time";
	private final static String ATTR_END_TIME = "end_time";
	private final static String ATTR_AMOUNT = "amount";

	private final static String YES = "yes";

	private NetworkLayer network = null;

	private RoadPricingScheme scheme = null;

	public RoadPricingReaderXMLv1(NetworkLayer network) {
		this.network = network;
	}

	public RoadPricingScheme getScheme() {
		return this.scheme;
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (TAG_ROADPRICING.equals(name)) {
			this.scheme = new RoadPricingScheme(this.network);
			this.scheme.setName(atts.getValue(ATTR_NAME));
			this.scheme.setType(atts.getValue(ATTR_TYPE));
			this.scheme.isActive(YES.equals(atts.getValue(ATTR_ACTIVE)));
		} else if (TAG_LINK.equals(name)) {
			this.scheme.addLink(atts.getValue(ATTR_ID));
		} else if (TAG_COST.equals(name)) {
			this.scheme.addCost(Time.parseTime(atts.getValue(ATTR_START_TIME)), 
					Time.parseTime(atts.getValue(ATTR_END_TIME)), Double.parseDouble(atts.getValue(ATTR_AMOUNT)));
		}
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (TAG_DESCRIPTION.equals(name)) {
			this.scheme.setDescription(content);
		}
	}

}
