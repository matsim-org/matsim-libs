/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdSampler.java
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

package playground.southafrica.population.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.households.Household;
import org.matsim.households.algorithms.HouseholdAlgorithm;


/**
 * Algorithm to sample a fraction of households, and pick <i>all</i> of the 
 * members of the sampled households. This ensures that the household structure
 * remains intact after sampling.
 * @author jwjoubert
 */
public class HouseholdSamplerAlgorithm implements HouseholdAlgorithm{
	private List<Id> membersToRemove;
	private List<Id> householdsToRemove;
	private final Random random;
	private final double fraction;
	
	
	public HouseholdSamplerAlgorithm(double fraction) {
		this(fraction, new Random());
	}
	
	
	public HouseholdSamplerAlgorithm(double fraction, Random random){
		this.householdsToRemove = new ArrayList<Id>();
		this.membersToRemove  = new ArrayList<Id>();
		this.random = random;
		this.fraction = fraction;
	}

	
	@Override
	public void run(Household household) {
		if(random.nextDouble() > fraction){
			householdsToRemove.add(household.getId());
			membersToRemove.addAll(household.getMemberIds());
		}
	}


	/**
	 * Returns all the household {@link Id}s that were <i>not<i/> sampled. This
	 * makes it easier to remove them. 
	 * @return
	 */
	public List<Id> getSampledIds(){
		return this.householdsToRemove;
	}
	
	
	/**
	 * Returns all the member {@link Id}s of the persons from households that
	 * were <i>not</i> sampled. This makes it easier to remove them.
	 * @return
	 */
	public List<Id> getSampledMemberIds(){
		return this.membersToRemove;
	}
	
}

