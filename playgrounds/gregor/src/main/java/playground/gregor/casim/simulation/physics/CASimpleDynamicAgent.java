/* *********************************************************************** *
 * project: org.matsim.*
 * CASimpleAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

//This class is for testing only will be removed in future [GL Nov '14]
@Deprecated
public class CASimpleDynamicAgent extends CAMoveableEntity {

	private CANetworkEntity currentEntity;

	private final List<Link> links;
	private int next;

	private int cnt = 0;

	private final Id<CASimpleDynamicAgent> id;

	private CANetworkEntity lastLink;

	public CASimpleDynamicAgent(List<Link> links, int next,
			Id<CASimpleDynamicAgent> id, CALink caLink) {
		this.links = links;
		this.next = next;
		this.id = id;
		this.currentEntity = caLink;
	}

	@Override
	Id<Link> getNextLinkId() {
		if (next < this.links.size()) {
			return this.links.get(this.next).getId();
		}
		return null;
	}

	@Override
	public void moveToNode(CANode n) {
		this.lastLink = this.currentEntity;
		this.currentEntity = n;
	}

	@Override
	void moveOverNode(CALink link, double time) {

		this.currentEntity = link;
		this.next++;
		// if (this.next == this.links.size()) {
		// this.next = this.links.size()-1;
		// this.letAgentArrive();
		// }
	}

	@Override
	public Link getCurrentLink() {
		return this.links.get(next - 1);
	}

	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		return this.currentEntity;
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public CANetworkEntity getLastCANetworkEntity() {
		return this.lastLink;
	}

}
