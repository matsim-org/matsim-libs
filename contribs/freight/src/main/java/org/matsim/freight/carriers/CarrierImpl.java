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

package org.matsim.freight.carriers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * This is a carrier that has capabilities and resources, jobs and plans to fulfill its obligations.
 * <p>
 *
 * @author sschroeder, mzilske
 *
 */
public final class CarrierImpl implements Carrier {
	@Deprecated // refactoring device, please inline
	public static Carrier newInstance( Id<Carrier> carrierId ){
		return CarriersUtils.createCarrier( carrierId ) ;
	}

	private final Id<Carrier> id;
	private final List<CarrierPlan> plans;
	private final Map<Id<CarrierShipment>, CarrierShipment> shipments;
	private final Map<Id<CarrierService>, CarrierService> services;
	private CarrierCapabilities carrierCapabilities;
	private CarrierPlan selectedPlan;
	private final Attributes attributes = new AttributesImpl();

	CarrierImpl( final Id<Carrier> id ) {
		super();
		this.carrierCapabilities = CarrierCapabilities.newInstance();
		this.id = id;
		services = new LinkedHashMap<>();
		shipments = new LinkedHashMap<>();
		plans = new ArrayList<>();
	}

	@Override
	public Id<Carrier> getId() {
		return id;
	}


	@Override
	public List<CarrierPlan> getPlans() {
		return plans;
	}

	@Override
	public Map<Id<CarrierShipment>, CarrierShipment> getShipments() {
		return shipments;
	}

	@Override
	public CarrierPlan getSelectedPlan() {
		return selectedPlan;
	}

	/**
	 * Adds a new CarrierPlan.
	 * Makes it selected if no other selectedPlan exists.
	 * @param carrierPlan carrierPlan to be added
	 */
	@Override
	public boolean addPlan(CarrierPlan carrierPlan) {
		// Make sure there is a selected carrierPlan if there is at least one carrierPlan
		if (this.selectedPlan == null) this.selectedPlan = carrierPlan;
		return this.plans.add(carrierPlan);
	}

	/**
	 * Set Plan as the selectedPlan.
	 * @param selectedPlan to be selected
	 */
	@Override
	public void setSelectedPlan(final CarrierPlan selectedPlan) {
		if (selectedPlan != null && !plans.contains( selectedPlan )) {
			throw new IllegalStateException("The plan to be set as selected is neither not null nor stored in the carrier's plans");
		}
		this.selectedPlan = selectedPlan;
	}

	@Override
	public void setCarrierCapabilities(CarrierCapabilities carrierCapabilities) {
		this.carrierCapabilities = carrierCapabilities;
	}

	@Override
	public CarrierCapabilities getCarrierCapabilities() {
		return carrierCapabilities;
	}

	@Override
	public Map<Id<CarrierService>, CarrierService> getServices(){
		return services;
	}



	@Override
	public CarrierPlan createCopyOfSelectedPlanAndMakeSelected() {
		CarrierPlan newPlan = CarriersUtils.copyPlan(this.selectedPlan ) ;
		this.addPlan( newPlan ) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan ;
	}

	@Override
	public boolean removePlan(CarrierPlan p) {
		return this.plans.remove(p);
	}

	@Override
	public void clearPlans() {
		this.plans.clear();
		this.selectedPlan = null;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

}
