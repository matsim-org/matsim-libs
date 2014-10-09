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

public class CASimpleDynamicAgent extends CAAgent {

	private CANetworkEntity currentEntity;

	private final List<Link> links;
	private int next;
	private final Id id;
	private CALink link;


	public CASimpleDynamicAgent(List<Link> links, int i, Id id, CALink caLink) {
		super(id);
		this.links = links;
		this.next = i;
		this.id = id;
		this.link = caLink;
		this.currentEntity = caLink;
	}

	@Override
	Id getNextLinkId() {

		return this.links.get(this.next).getId();
	}

	@Override
	public void moveToNode(CANode n) {
		this.currentEntity = n;
	}

	@Override
	void moveOverNode(CALink link, double time) {
		this.link = link;
		this.currentEntity = link;
		this.next++;
		if (this.next == this.links.size()) {
			this.next = this.links.size()-1;
		}
	}



	@Override
	public CALink getCurrentLink() {
		return this.link;
	}



	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		return this.currentEntity;
	}

	

}
