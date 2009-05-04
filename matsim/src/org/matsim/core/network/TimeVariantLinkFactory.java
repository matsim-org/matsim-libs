/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;

public class TimeVariantLinkFactory implements LinkFactory {

	public Link createLink(Id id, Node from, Node to, Network network, double length, double freespeedTravelTime,
			double capacity, double nOfLanes) {
		return new TimeVariantLinkImpl(id, from, to, (NetworkLayer) network, length, freespeedTravelTime, capacity, nOfLanes);
	}

}
