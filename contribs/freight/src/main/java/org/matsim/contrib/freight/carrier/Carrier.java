/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.carrier;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A carrier.
 * 
 * @author sschroeder
 *
 */
public interface Carrier extends HasPlansAndId<CarrierPlan, Carrier>, Attributable{
	
	public static int PLAN_MEMORY = 5;

	/**
	 * Gets the carrierId.
	 * 
	 * @return id
	 */
	@Override
	public abstract Id<Carrier> getId();

	/**
	 * Gets a collection of carrierPlans.
	 * 
	 * @return collection of {@link CarrierPlan}
	 */
	@Override
	public abstract List<CarrierPlan> getPlans();

	/**
	 * Gets a collection of carrierShipments
	 * 
	 * @return collection of {@link CarrierShipment}
	 */
	public abstract Map<Id<CarrierShipment>, CarrierShipment> getShipments();
	
	/**
	 * Gets a collection of carrierServices
	 * 
	 * @return collection of {@link CarrierService}
	 */
	public abstract Map<Id<CarrierService>, CarrierService> getServices();

	/**
	 * Gets the selected plan.
	 * 
	 * @return {@link CarrierPlan}
	 */
	@Override
	public abstract CarrierPlan getSelectedPlan();

	/**
	 * Sets a {@link CarrierPlan} as selected.
	 * 
	 * <p>If selectePlan in not in plan-collection, it adds it.
	 * 
	 * @param selectedPlan to be set
	 */
	@Override
	public abstract void setSelectedPlan(CarrierPlan selectedPlan);

	/**
	 * Sets carrierCapabilities.
	 * 
	 * @param carrierCapabilities to be set
	 */
	public abstract void setCarrierCapabilities(CarrierCapabilities carrierCapabilities);

	/**
	 * Gets the carrierCapabilities.
	 * 
	 * @return {@link CarrierCapabilities}
	 */
	public abstract CarrierCapabilities getCarrierCapabilities();


	void clearPlans();
}
