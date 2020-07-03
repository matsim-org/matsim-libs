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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import gnu.trove.map.TObjectDoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * Contains the relevant information for a single link, i.e. time-specific data such as number of vehicles, noise emission or damages.
 * 
 * @author ikaddoura
 *
 */
final class NoiseLink {

	private final Id<Link> id;

	private List<Id<Vehicle>> enteringVehicleIds = new ArrayList<>();

	private final Map<Id<? extends NoiseVehicleType>, Double> travelTimeByType = new HashMap<>();

	private final Map<Id<? extends NoiseVehicleType>, Integer> vehiclesEnteringByType = new HashMap<>();
	private final Map<Id<? extends NoiseVehicleType>, Integer> vehiclesLeavingByType = new HashMap<>();

	private final Map<Id<? extends NoiseVehicleType>, Double> marginalEmissionIncreases = new HashMap<>();
	private final Map<Id<? extends NoiseVehicleType>, Double> marginalImmissionIncreases = new HashMap<>();

	private final Map<Id<? extends NoiseVehicleType>, Double> marginalDamageCosts = new HashMap<>();
	private final Map<Id<? extends NoiseVehicleType>, Double> averageDamageCosts = new HashMap<>();

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

	void setEnteringVehicleIds(List<Id<Vehicle>> enteringVehicleIds) {
		this.enteringVehicleIds = enteringVehicleIds;
	}

	public int getAgentsEntering(Id<NoiseVehicleType> typeId) {
		return vehiclesEnteringByType.computeIfAbsent(typeId, id ->0);
	}

	public void setAgentsEntering(Id<NoiseVehicleType> id, int agentsEntering) {
		this.vehiclesEnteringByType.put(id, agentsEntering);
	}

	int getAgentsLeaving(Id<NoiseVehicleType> typeId) {
		return vehiclesLeavingByType.computeIfAbsent(typeId, id -> 0);
	}

	void setAgentsLeaving(Id<NoiseVehicleType> id, int agentsLeaving) {
		this.vehiclesLeavingByType.put(id, agentsLeaving);
	}

	double getDamageCost() {
		return damageCost;
	}

	void setDamageCost(double damageCost) {
		this.damageCost = damageCost;
	}

	public double getEmission() {
		return emission;
	}

	public void setEmission(double emission) {
		this.emission = emission;
	}

	double getEmissionPlusOneVehicle(Id<NoiseVehicleType> typeId) {
		return marginalEmissionIncreases.computeIfAbsent(typeId, id -> 0.);
	}

	void setEmissionPlusOneVehicle(Id<NoiseVehicleType> id, double emissionPlusOneVehicle) {
		this.marginalEmissionIncreases.put(id, emissionPlusOneVehicle);
	}

	public double getImmissionPlusOneVehicle(Id<NoiseVehicleType> typeId) {
		return marginalImmissionIncreases.computeIfAbsent(typeId, id -> 0.);
	}

	public void setImmissionPlusOneCar(Id<NoiseVehicleType> id, double immissionPlusOneVehicle) {
		this.marginalImmissionIncreases.put(id, immissionPlusOneVehicle);
	}

	public double getMarginalDamageCostPerVehicle(Id<NoiseVehicleType> typeId) {
		return marginalDamageCosts.computeIfAbsent(typeId, id -> 0.);
	}

	public void setMarginalDamageCostPerVehicle(Id<NoiseVehicleType> id, double marginalDamageCostPerVehicle) {
		this.marginalDamageCosts.put(id, marginalDamageCostPerVehicle);
	}

	double getAverageDamageCostPerVehicle(Id<NoiseVehicleType> typeId) {
		return averageDamageCosts.computeIfAbsent(typeId, id -> 0.);
	}

	public void setAverageDamageCostPerVehicle(Id<NoiseVehicleType> id, double damageCostPerVehicle) {
		this.averageDamageCosts.put(id, damageCostPerVehicle);
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

	void setTravelTime(Id<NoiseVehicleType> type, double time_sec) {
		this.travelTimeByType.put(type, time_sec);
	}

	double getTravelTime_sec(Id<NoiseVehicleType> type) {
		return this.travelTimeByType.computeIfAbsent(type, id -> 0.);
	}
}
