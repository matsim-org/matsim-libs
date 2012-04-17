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
package playground.droeder.eMobility.v3;

import org.matsim.api.core.v01.Scenario;

import playground.droeder.eMobility.v3.fleet.EFleet;
import playground.droeder.eMobility.v3.poi.PoiInfo;
import playground.droeder.eMobility.v3.population.EPopulation;

/**
 * @author droeder
 *
 */
public class EmobilityScenario {
	
	private Scenario sc;
	private EFleet fleet;
	private EPopulation population;
	private PoiInfo poi;
	
	public EmobilityScenario(){
		
	}

	/**
	 * @return the sc
	 */
	public Scenario getSc() {
		return sc;
	}

	/**
	 * @param sc the sc to set
	 */
	public void setSc(Scenario sc) {
		this.sc = sc;
	}

	/**
	 * @return the fleet
	 */
	public EFleet getFleet() {
		return fleet;
	}

	/**
	 * @param fleet the fleet to set
	 */
	public void setFleet(EFleet fleet) {
		this.fleet = fleet;
	}

	/**
	 * @return the population
	 */
	public EPopulation getPopulation() {
		return population;
	}

	/**
	 * @param population the population to set
	 */
	public void setPopulation(EPopulation population) {
		this.population = population;
	}

	/**
	 * @return
	 */
	public PoiInfo getPoi() {
		return this.poi;
	}
	
	public void setPoi(PoiInfo poi){
		this.poi = poi;
	}
}
