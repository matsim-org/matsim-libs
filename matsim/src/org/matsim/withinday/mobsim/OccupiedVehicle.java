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

package org.matsim.withinday.mobsim;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.Node;
import org.matsim.withinday.WithindayAgent;

/**
 * @author dgrether
 * 
 */
public class OccupiedVehicle extends Vehicle {

//	private static final Logger log = Logger.getLogger(OccupiedVehicle.class);
	
	private WithindayAgent agent;

	public OccupiedVehicle() {
		super();
	}

	@Override
	public QueueLink chooseNextLink() {
		this.agent.replan();
		QueueLink l = super.chooseNextLink(); 
//		log.trace("vehicle : " + this.driverId + " next choosen link:" + l.getId().asString());
		return l;
	}

	public void exchangeActsLegs(final ArrayList<Object> actslegs) {
		this.cachedNextLink = null;
		int index = this.actslegs.indexOf(this.currentLeg);
		if (index != -1) {
			this.actslegs = actslegs;
			this.currentLeg = (BasicLeg) this.actslegs.get(index);
		}		
		else {
			throw new IllegalArgumentException("Current leg: " + this.currentLeg
					+ " can not be found in actslegs list!");
		}
	}

	/**
	 * @return the agent
	 */
	public WithindayAgent getAgent() {
		return this.agent;
	}

	/**
	 * @param agent
	 *          the agent to set
	 */
	public void setAgent(final WithindayAgent agent) {
		this.agent = agent;
	}

	public Node getCurrentNode() {
		return this.currentLink.getToNode();
	}

}
