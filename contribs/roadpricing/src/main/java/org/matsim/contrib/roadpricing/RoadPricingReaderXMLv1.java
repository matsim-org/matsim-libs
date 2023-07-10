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

package org.matsim.contrib.roadpricing;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

/**
 * Reads XML files containing a {@link RoadPricingSchemeImpl} according to <code>roadpricing_v1.dtd</code>.
 *
 * @author mrieser
 */
public final class RoadPricingReaderXMLv1 extends MatsimXmlParser  {
	// currently needs to be public. kai, sep'14

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

	private RoadPricingSchemeImpl scheme;

	private Id<Link> currentLinkId = null;

	private boolean hasLinkCosts = false;

	public RoadPricingReaderXMLv1(RoadPricingSchemeImpl scheme){
		super(ValidationType.DTD_ONLY);
		this.scheme = scheme;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (TAG_ROADPRICING.equals(name)) {
			this.scheme.setName(atts.getValue(ATTR_NAME));
			this.scheme.setType(atts.getValue(ATTR_TYPE));
		} else if (TAG_LINK.equals(name)) {
			this.currentLinkId = Id.create(atts.getValue(ATTR_ID), Link.class);
		} else if (TAG_COST.equals(name)) {
			// an undefined toll start time == -Inf
			double startTime = Time.parseOptionalTime(atts.getValue(ATTR_START_TIME)).orElse(Double.NEGATIVE_INFINITY);
			// an undefined toll end time == +Inf
			double endTime = Time.parseOptionalTime(atts.getValue(ATTR_END_TIME)).orElse(Double.POSITIVE_INFINITY);
			double amount = Double.parseDouble(atts.getValue(ATTR_AMOUNT));
			if (this.currentLinkId == null) {
				this.scheme.createAndAddCost(startTime, endTime, amount);
			} else {
				this.scheme.addLinkCost(this.currentLinkId, startTime, endTime, amount);
				this.hasLinkCosts = true;
			}
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (TAG_DESCRIPTION.equals(name)) {
			this.scheme.setDescription(content);
		}
		else if (TAG_LINK.equals(name)){
			if (!hasLinkCosts){
				this.scheme.addLink(this.currentLinkId);
			}
			this.currentLinkId = null;
			this.hasLinkCosts = false;
		}
	}

}
