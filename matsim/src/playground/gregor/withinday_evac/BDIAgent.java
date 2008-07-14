/* *********************************************************************** *
 * project: org.matsim.*
 * BDIAgent.java
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

package playground.gregor.withinday_evac;

import org.matsim.plans.Person;

import playground.gregor.withinday_evac.mobsim.OccupiedVehicle;


public class BDIAgent {

	private final Person person;
	private final OccupiedVehicle vehicle;

	public BDIAgent(Person person, OccupiedVehicle v){
		this.person = person;
		this.vehicle = v;
		this.vehicle.setAgent(this);
		
	}

	public void replan() {
		// TODO Auto-generated method stub
		
	}
}
