/* *********************************************************************** *
 * project: org.matsim.*
 * OccupiedVehicle.java
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

package playground.gregor.withinday_evac.mobsim;

import org.matsim.basic.v01.Id;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Link;
import org.matsim.network.Node;

import playground.gregor.withinday_evac.BDIAgent;

/**
 * @author dgrether
 *
 */
public class OccupiedVehicle extends Vehicle {

//	private static final Logger log = Logger.getLogger(OccupiedVehicle.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BDIAgent agent;

	public OccupiedVehicle() {
		super();
	}


	public void setNextLink(final double now, final Id nodeId) {
		final Link l = this.agent.replan(now, nodeId);
		super.cachedNextLink = l;

	}

	
	@Override
	public Link chooseNextLink() {
		return super.chooseNextLink();
	}

//	public void exchangeActsLegs(final ArrayList<Object> actslegs) {
//		this.cachedNextLink = null;
//		final int index = this.actslegs.indexOf(this.currentLeg);
//		if (index != -1) {
//			this.actslegs = actslegs;
//			this.currentLeg = (BasicLeg) this.actslegs.get(index);
//		}
//		else {
//			throw new IllegalArgumentException("Current leg: " + this.currentLeg
//					+ " can not be found in actslegs list!");
//		}
//	}



	/**
	 * @return the agent
	 */
	public BDIAgent getAgent() {
		return this.agent;
	}

	/**
	 * @param agent
	 *          the agent to set
	 */
	public void setAgent(final BDIAgent agent) {
		this.agent = agent;
	}

	public Node getCurrentNode() {
		return this.currentLink.getToNode();
	}

}
