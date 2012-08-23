/* *********************************************************************** *
 * project: org.matsim.*
 * BasicHouseholdsImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.algorithms.HouseholdAlgorithm;


/**
 * Basic implementation of the Households container, a pure data class
 * 
 * @author dgrether
 */
public class HouseholdsImpl implements Households{
	
	private final static Logger LOG = Logger.getLogger(HouseholdImpl.class);

	@Deprecated 
	private boolean isStreaming = false;
	
	@Deprecated 
	private Counter counter = new Counter (" household # ");
	@Deprecated 
	private final List<HouseholdAlgorithm> householdAlgorithms = new ArrayList<HouseholdAlgorithm>();

	private HouseholdsFactory factory;

	private Map<Id, Household> households;
	
	
	public HouseholdsImpl(){
		this.households = new HashMap<Id, Household>();
		this.factory = new HouseholdsFactoryImpl();
	}
	
	
	/**
	 * Adds the household to the container. If streaming is set, the household
	 * is added, the algorithms are run on the household, and it is subsequently
	 * removed. If not, the household is added incrementally to the container.
	 * @param household
	 * @throws IllegalArgumentException if the container already includes the 
	 * 		{@link Id} of the household being added.
	 */
	public final void addHousehold(final Household household){
		/* Validate that a household with the same Id does not exist yet. */
		if(this.getHouseholds().containsKey(household.getId())){
			throw new IllegalArgumentException("Household with Id " + household.getId() + 
					" already exisits.");
		}
		counter.incCounter();
		
		if(!isStreaming){
			/* Streaming is off: just add the household to the container. */
			this.households.put(household.getId(), household);
		} else{
			/* Streaming is in: run algorithm(s) on household and remove it */
			
			/* Add the household, for algorithms might reference the household
			 * through `household = Households.getHouseholds.get(hhId);' */
			this.households.put(household.getId(), household);
			
			/* Run each of the algorithms */
			for(HouseholdAlgorithm algorithm : this.householdAlgorithms){
				algorithm.run(household);
			}
			
			/* Remove the household again as we are streaming. */
			this.getHouseholds().remove(household.getId());
		}
	}
	
	
	/**
	 * Run all the algorithms added to the container. 
	 * @deprecated use HouseholdsAlgorithmRunner instead
	 */
	@Deprecated
	public final void runAlgorithms(){
		if(!this.isStreaming){
			for(int i = 0; i < this.householdAlgorithms.size(); i++){
				HouseholdAlgorithm algorithm = this.householdAlgorithms.get(i);
				LOG.info("Running algorithm " + algorithm.getClass().getName());
				Counter c = new Counter ("  household # ");
				for(Household household : this.getHouseholds().values()){
					algorithm.run(household);
					c.incCounter();
				}
				c.printCounter();
				LOG.info("Done running algorithm.");
			}
		} else{
			LOG.info("Household streaming is on. Algorithms were run during parsing.");
		}
	}
	
	
	/**
	 * Removes all the algorithms from the Households container.
	 * @deprecated use HouseholdsAlgorithmRunner instead
	 */
	@Deprecated
	public final void clearAlgorithms(){
		this.householdAlgorithms.clear();
	}
	
	
	/**
	 * Removes the first instance found of the algorithm from the list. It is 
	 * possible that the same algorithm can appear multiple times in the list.
	 * @param algorithm
	 * @return
	 * 	@deprecated use HouseholdsAlgorithmRunner instead
	 */
	@Deprecated
	public boolean removeAlgorithm(final HouseholdAlgorithm algorithm){
		return this.householdAlgorithms.remove(algorithm);
	}
	
	
	/**
	 * Add the algorithm to the container. Algorithms will be executed in the
	 * same sequence in which they are added.
	 * @param algorithm
	 * @deprecated use HouseholdsAlgorithmRunner instead
	 */
	@Deprecated
	public final void addAlgorithm(final HouseholdAlgorithm algorithm){
		this.householdAlgorithms.add(algorithm);
	}
	
	@Override
	public HouseholdsFactory getFactory() {
		return this.factory;
	}

	public void setFactory(HouseholdsFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public Map<Id, Household> getHouseholds() {
		return this.households;
	}
	
	/**
	 * @deprecated use HouseholdsStreamingReaderV10 instead
	 */
	@Deprecated
	public final boolean isStreaming(){
		return this.isStreaming;
	}
	
	
	/**
	 * Default is <code>false</code>. Set to <code>true</code> if you do not 
	 * want to accumulate the {@link Household}s, but rather execute
	 * a set of {@link HouseholdAlgorithm}s while reading the a households file.  
	 * @param isStreaming
	 * @deprecated use HouseholdsStreamingReaderV10 instead
	 */
	@Deprecated
	public final void setStreaming(final boolean isStreaming){
		this.isStreaming = isStreaming;
	}
	
	/**
	 * Prints the current value of the households counter. This should be the
	 * same as the number of households in the container.
	 * @deprecated could be invoked directly on this.counter
	 */
	@Deprecated
	/*package*/ void printCounter(){
		this.counter.printCounter();
	}

}
