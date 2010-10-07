/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkImpl;

public class WithinDayLinkFactoryImpl implements LinkFactory{

	@Override
	public WithinDayLinkImpl createLink(Id id, Node from, Node to, NetworkImpl network, double length, double freespeed, double capacity, double nOfLanes) {
		return new WithinDayLinkImpl(id, from, to, network, length, freespeed, capacity, nOfLanes);
	}
}
