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
package playground.droeder.eMobility.v2.population;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import playground.droeder.eMobility.v2.fleet.EmobFleet;

/**
 * @author droeder
 *
 */
public class EmobPopulationReader {
	
	private Network net;
	private EmobPopulation pop;
	private EmobFleet fleet;

	public EmobPopulationReader(Network net){
		this.net = net;
	}
	
	public void read(String inFile){
		
	}

	public EmobPopulation getPopulation(){
		return this.pop;
	}
	
	public void add2MATSimPopulation(Population p){

	}
	
	public EmobFleet getFleet(){
		return this.fleet;
	}
}
