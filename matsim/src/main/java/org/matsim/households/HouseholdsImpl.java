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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * Basic implementation of the Households container, a pure data class
 *
 * @author dgrether
 */
public class HouseholdsImpl implements Households {

	private HouseholdsFactory factory;

	private final Map<Id<Household>, Household> households;

	public HouseholdsImpl(){
		this.households = new LinkedHashMap<>();
		this.factory = new HouseholdsFactoryImpl();
	}


	/**
	 * Adds the household to the container. If streaming is set, the household
	 * is added, the algorithms are run on the household, and it is subsequently
	 * removed. If not, the household is added incrementally to the container.
	 *
	 * @throws IllegalArgumentException if the container already includes the
	 * 		{@link Id} of the household being added.
	 */
	public final void addHousehold(final Household household){
		/* Validate that a household with the same Id does not exist yet. */
		if(this.getHouseholds().containsKey(household.getId())){
			throw new IllegalArgumentException("Household with Id " + household.getId() +
					" already exisits.");
		}
		this.households.put(household.getId(), household);
	}

	@Override
	public HouseholdsFactory getFactory() {
		return this.factory;
	}

	public void setFactory(HouseholdsFactory factory) {
		this.factory = factory;
	}

	@Override
	public Map<Id<Household>, Household> getHouseholds() {
		return this.households;
	}

}
