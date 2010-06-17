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

package playground.mrieser.core.sim.impl;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;

import playground.mrieser.core.sim.api.DriverAgent;

public class NetworkRouteDriver implements DriverAgent {

	private final Id[] linkIds;
	private int nextLinkIndex = 0;
	private Id nextLinkId = null;

	public NetworkRouteDriver(final NetworkRoute route) {
		List<Id> tmpIds = route.getLinkIds();
		this.linkIds = new Id[3 + tmpIds.size()];
		this.linkIds[0] = route.getStartLinkId();
		int index = 1;
		for (Id id : tmpIds) {
			this.linkIds[index] = id;
			index++;
		}
		this.linkIds[index] = route.getEndLinkId();
		this.linkIds[index+1] = null; // sentinel
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
	}

	@Override
	public Id getNextLinkId() {
		return this.nextLinkId;
	}

	@Override
	public void notifyMoveToNextLink() {
		this.nextLinkIndex++;
		if (this.nextLinkIndex == this.linkIds.length) {
			this.nextLinkIndex--;
		}
		this.nextLinkId = this.linkIds[this.nextLinkIndex];
	}

}
