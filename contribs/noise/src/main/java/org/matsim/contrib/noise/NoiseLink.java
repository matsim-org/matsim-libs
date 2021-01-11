/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Contains the relevant information for a single link, i.e. time-specific data such as number of vehicles, noise emission or damages.
 * 
 * @author ikaddoura
 *
 */
final class NoiseLink {

	private final Id<Link> id;

	private List<Id<Vehicle>> enteringVehicleIds = null;

	private final Map<NoiseVehicleType, Double> travelTimeByType = new HashMap<>();

	private final Multiset<NoiseVehicleType> vehiclesEnteringByType = HashMultiset.create();
	private final Multiset<NoiseVehicleType> vehiclesLeavingByType = HashMultiset.create();

	private final Map<NoiseVehicleType, Double> marginalEmissionIncreases = new HashMap<>();
	private final Map<NoiseVehicleType, Double> marginalImmissionIncreases = new HashMap<>();

	private final Map<NoiseVehicleType, Double> marginalDamageCosts = new HashMap<>();
	private final Map<NoiseVehicleType, Double> averageDamageCosts = new HashMap<>();

	private double emission = 0.;
	private double damageCost = 0.; 


	NoiseLink(Id<Link> linkId) {
		this.id = linkId;
	}

	public Id<Link> getId() {
		return id;
	}

	List<Id<Vehicle>> getEnteringVehicleIds() {
		return enteringVehicleIds;
	}

	void addEnteringVehicleId(Id<Vehicle> enteringVehicleId) {
		if(this.enteringVehicleIds == null) {
			enteringVehicleIds = new ArrayList<>();
		}
		enteringVehicleIds.add(enteringVehicleId);
	}

	int getAgentsEntering(NoiseVehicleType type) {
		return vehiclesEnteringByType.count(type);
	}

	void addEnteringAgent(NoiseVehicleType type) {
		this.vehiclesEnteringByType.add(type);
	}

	int getAgentsLeaving(NoiseVehicleType type) {
		return vehiclesLeavingByType.count(type);
	}

	void addLeavingAgent(NoiseVehicleType type) {
		this.vehiclesLeavingByType.add(type);
	}

	double getDamageCost() {
		return damageCost;
	}

	synchronized void addDamageCost(double damageCost) {
		this.damageCost += damageCost;
	}

	public double getEmission() {
		return emission;
	}

	public void setEmission(double emission) {
		this.emission = emission;
	}

	double getEmissionPlusOneVehicle(NoiseVehicleType type) {
		return marginalEmissionIncreases.getOrDefault(type, 0.);
	}

	void setEmissionPlusOneVehicle(NoiseVehicleType type, double emissionPlusOneVehicle) {
		this.marginalEmissionIncreases.put(type, emissionPlusOneVehicle);
	}

//	public double getImmissionPlusOneVehicle(Id<NoiseVehicleType> typeId) {
//		return marginalImmissionIncreases.computeIfAbsent(typeId, id -> 0.);
//	}
//
//	public void setImmissionPlusOneCar(Id<NoiseVehicleType> id, double immissionPlusOneVehicle) {
//		this.marginalImmissionIncreases.put(id, immissionPlusOneVehicle);
//	}

	double getMarginalDamageCostPerVehicle(NoiseVehicleType type) {
		return marginalDamageCosts.getOrDefault(type, 0.);
	}

	synchronized void addMarginalDamageCostPerVehicle(NoiseVehicleType type, double marginalDamageCostPerVehicle) {
		this.marginalDamageCosts.merge(type, marginalDamageCostPerVehicle, Double::sum);
	}

	synchronized double getAverageDamageCostPerVehicle(NoiseVehicleType type) {
		return averageDamageCosts.getOrDefault(type, 0.);
	}

	void setAverageDamageCostPerVehicle(NoiseVehicleType type, double damageCostPerVehicle) {
		this.averageDamageCosts.put(type, damageCostPerVehicle);
	}

	@Override
	public String toString() {
		return "NoiseLink [id=" + id + ", enteringVehicleIds="
				+ enteringVehicleIds + ", emission=" + emission;
//				+ ", emissionPlusOneCar=" + emissionPlusOneCar
//				+ ", emissionPlusOneHGV=" + emissionPlusOneHGV
//				+ ", immissionPlusOneCar=" + immissionPlusOneCar
//				+ ", immissionPlusOneHGV=" + immissionPlusOneHGV
//				+ ", damageCost=" + damageCost + ", averageDamageCostPerCar="
//				+ averageDamageCostPerCar + ", averageDamageCostPerHgv="
//				+ averageDamageCostPerHgv + ", marginalDamageCostPerCar="
//				+ marginalDamageCostPerCar + ", marginalDamageCostPerHgv="
//				+ marginalDamageCostPerHgv + ", travelTimeCar="
//				+ travelTimeCar_Sec + ", travelTimeHGV="
//				+ travelTimeHGV_Sec + "]";
	}

	void setTravelTime(NoiseVehicleType type, double time_sec) {
		this.travelTimeByType.put(type, time_sec);
	}

	double getTravelTime_sec(NoiseVehicleType type) {
		return this.travelTimeByType.getOrDefault(type, 0.);
	}
}
