/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.cadyts.car;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import utilities.misc.DynamicDataXMLFileIO;

/**
 * Enables cadyts to persist the cost offsets to file.
 */
class CadytsLinkCostOffsetsXMLFileIO extends DynamicDataXMLFileIO<Link> {

	private static final long serialVersionUID = 1L;
	private final Network network;

	public CadytsLinkCostOffsetsXMLFileIO(final Network network) {
		super();
		this.network = network;
	}

	@Override
	protected Link attrValue2key(final String linkIdString) {
		Link link = this.network.getLinks().get(new IdImpl(linkIdString));
		return link;
	}

	@Override
	protected String key2attrValue(final Link key) {
		return key.getId().toString();
	}

}