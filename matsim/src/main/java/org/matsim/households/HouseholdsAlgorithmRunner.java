/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsAlgorithmRunner
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
package org.matsim.households;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.algorithms.HouseholdAlgorithm;


/**
 * @author jjoubert
 * @author dgrether
 *
 */
public class HouseholdsAlgorithmRunner {
	
	private static final Logger log = LogManager.getLogger(HouseholdsAlgorithmRunner.class);
	
	private final List<HouseholdAlgorithm> householdAlgorithms = new ArrayList<HouseholdAlgorithm>();

	/**
	 * Run all the algorithms added to the container. 
	 */
	public final void runAlgorithms(Households hh){
			for(int i = 0; i < this.householdAlgorithms.size(); i++){
				HouseholdAlgorithm algorithm = this.householdAlgorithms.get(i);
				log.info("Running algorithm " + algorithm.getClass().getName());
				Counter c = new Counter ("  household # ");
				for(Household household : hh.getHouseholds().values()){
					algorithm.run(household);
					c.incCounter();
				}
				c.printCounter();
				log.info("Done running algorithm.");
			}
	}

	public void runAlgorithms(Household h){
		for(HouseholdAlgorithm algorithm : this.householdAlgorithms){
			log.info("Running algorithm " + algorithm.getClass().getName() + " on household " + h.getId());
			algorithm.run(h);
		}		
	}
	
	
	/**
	 * Removes all the algorithms from the Households container.
	 */
	public final void clearAlgorithms(){
		this.householdAlgorithms.clear();
	}
	
	/**
	 * Removes the first instance found of the algorithm from the list. It is 
	 * possible that the same algorithm can appear multiple times in the list.
	 * @param algorithm
	 * @return
	 */
	public boolean removeAlgorithm(final HouseholdAlgorithm algorithm){
		return this.householdAlgorithms.remove(algorithm);
	}
	
	
	/**
	 * Add the algorithm to the container. Algorithms will be executed in the
	 * same sequence in which they are added.
	 * @param algorithm
	 */
	public final void addAlgorithm(final HouseholdAlgorithm algorithm){
		this.householdAlgorithms.add(algorithm);
	}
}
