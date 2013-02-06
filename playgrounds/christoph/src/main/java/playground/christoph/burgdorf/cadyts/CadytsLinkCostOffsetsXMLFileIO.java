/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsLinkCostOffsetsXMLFileIO.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf.cadyts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import utilities.misc.DynamicDataXMLFileIO;

/**
 * Enables cadyts to persist the cost offsets to file.
 */
public class CadytsLinkCostOffsetsXMLFileIO extends DynamicDataXMLFileIO<Link> {

	private static final long serialVersionUID = 1L;
	private final Scenario scenario;

	public CadytsLinkCostOffsetsXMLFileIO(final Scenario scenario) {
		super();
		this.scenario = scenario;
	}

	@Override
	protected Link attrValue2key(final String linkIdString) {
		Link link = this.scenario.getNetwork().getLinks().get(scenario.createId(linkIdString));
		return link;
	}

	@Override
	protected String key2attrValue(final Link key) {
		return key.getId().toString();
	}

}