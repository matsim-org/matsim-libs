/* *********************************************************************** *
 * project: org.matsim.*
 * CAVehicle.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

public class CAVehicle extends CAAgent {

	private final MobsimDriverAgent agent;
	private final Id initialLinkId;
	private CALink currentLink;

	public CAVehicle(Id id, MobsimDriverAgent agent, Id linkId, CALink current) {
		super(id);
		this.agent = agent;
		this.initialLinkId = linkId;
		this.currentLink = current;
	}

	@Override
	Id getNextLinkId() {
		return this.agent.chooseNextLinkId();
	}

	@Override
	void moveOverNode(CALink nextLink, double time) {
		this.agent.notifyMoveOverNode(nextLink.getLink().getId());
		this.currentLink = nextLink;

	}
	
	/*package*/ Id getInitialLinkId() {
		return this.initialLinkId;
	}

	@Override
	CALink getCurrentLink() {
		return this.currentLink;
	}

	@Override
	public double getZ() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getD() {
		// TODO Auto-generated method stub
		return 0;
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

	@Override
	public double getCumWaitTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCumWaitTime(double tFree) {
		// TODO Auto-generated method stub
		
	}

}
