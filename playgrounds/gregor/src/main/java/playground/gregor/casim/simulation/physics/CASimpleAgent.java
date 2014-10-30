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

public class CASimpleAgent extends CAAgent {

	static  double Z = 0;
	static  double D = 0;
	private final List<Link> links;
	private int next;
	private final Id<CAAgent> id;
	private CALink link;

	public CASimpleAgent(List<Link> links, int i, Id<CAAgent> id, CALink caLink) {
		super(id);
		this.links = links;
		this.next = i;
		this.id = id;
		this.link = caLink;
	}

	@Override
	Id<Link> getNextLinkId() {
		
		return this.links.get(this.next).getId();
	}

	@Override
	void moveOverNode(CALink link, double time) {
//		System.out.println("DEBUG");
//		if (this.id.toString().equals("46") && link.getLink().getId().toString().equals("7") && time > 300){
//			System.out.println("DEBUG");
//		}
		this.link = link;
		this.next++;
//		System.out.println(link.getLink().getId() + " " + this.links.get(this.next).getId());
		if (this.next == this.links.size()) {
			this.next = 4;
		}
//		System.out.println(this.next);
	}

	@Override
	public String toString() {
		return "a:"+this.id;
	}
		
	
	@Override
	public CALink getCurrentLink() {
		return this.link;
	}


	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void moveToNode(CANode n) {
		// TODO Auto-generated method stub
		
	}

}
