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

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	 * Selects the selectedPlan.
	 *
	 * <p> If the plan-collection does not contain the selectedPlan, it is added to that collection.
	 *
	 * @param selectedPlan to be selected
	 */
	@Override
	public void setSelectedPlan(CarrierPlan selectedPlan) {
		if(!plans.contains(selectedPlan)) plans.add(selectedPlan);
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
	public boolean addPlan(CarrierPlan p) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public CarrierPlan createCopyOfSelectedPlanAndMakeSelected() {
		CarrierPlan newPlan = CarriersUtils.copyPlan(this.selectedPlan ) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan ;
	}

	@Override
	public boolean removePlan(CarrierPlan p) {
		return this.plans.remove(p);
	}

	@Override
	public void clearPlans() { this.plans.clear(); }

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

}
