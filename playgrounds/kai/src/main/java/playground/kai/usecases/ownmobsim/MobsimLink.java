/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.kai.usecases.ownmobsim;

import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;

class MobsimLink {
	
	Queue<DriverVehicleUnit> driveway = new PriorityQueue<DriverVehicleUnit>() ;
	
	MobsimLink(Link link) {
		// TODO Auto-generated constructor stub
	}

	public void addToParking(DriverVehicleUnit mp) {
		
	}

	public void doSimStep() {
		// TODO Auto-generated method stub
		
	}

	public DriverVehicleUnit peek() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasSpace() {
		// TODO Auto-generated method stub
		return false;
	}

	public void remove() {
		// TODO Auto-generated method stub
		
	}

	public void addFromIntersection(DriverVehicleUnit vehicle) {
		// TODO Auto-generated method stub
		
	}

}
