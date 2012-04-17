/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.eMobility.population;

import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.droeder.eMobility.fleet.EVehicle;

/**
 * @author droeder
 *
 */
public class EPerson {
	
	private EVehicle vehicle;
	private Id id;

	public EPerson(Id id, EVehicle vehicle){
		this.vehicle = vehicle;
		this.id = id;
	}
	
	public EVehicle getVehicle(){
		return this.vehicle;
	}

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}


	
}
