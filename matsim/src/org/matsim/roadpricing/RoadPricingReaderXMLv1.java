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

import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;

/**
 * Reads XML files containing a {@link RoadPricingScheme} according to <code>roadpricing_v1.dtd</code>.
 *
 * @author mrieser
 */
public class RoadPricingReaderXMLv1 extends MatsimXmlParser  {

	private final static String TAG_ROADPRICING = "roadpricing";
	private final static String TAG_DESCRIPTION = "description";
	private final static String TAG_LINK = "link";
	private final static String TAG_COST = "cost";

	private final static String ATTR_NAME = "name";
	private final static String ATTR_TYPE = "type";
	private final static String ATTR_ID = "id";
	private final static String ATTR_START_TIME = "start_time";
	private final static String ATTR_END_TIME = "end_time";
	private final static String ATTR_AMOUNT = "amount";

	private Network network = null;

	private RoadPricingScheme scheme = null;

	public RoadPricingReaderXMLv1(final Network network) {
		this.network = network;
	}

	public RoadPricingScheme getScheme() {
		return this.scheme;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (TAG_ROADPRICING.equals(name)) {
			this.scheme = new RoadPricingScheme(this.network);
			this.scheme.setName(atts.getValue(ATTR_NAME));
			this.scheme.setType(atts.getValue(ATTR_TYPE));
		} else if (TAG_LINK.equals(name)) {
			this.scheme.addLink(atts.getValue(ATTR_ID));
		} else if (TAG_COST.equals(name)) {
			this.scheme.addCost(Time.parseTime(atts.getValue(ATTR_START_TIME)),
					Time.parseTime(atts.getValue(ATTR_END_TIME)), Double.parseDouble(atts.getValue(ATTR_AMOUNT)));
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (TAG_DESCRIPTION.equals(name)) {
			this.scheme.setDescription(content);
		}
	}

}
