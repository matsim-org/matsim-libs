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
package org.matsim.contrib.noise.data;

import java.util.ArrayList;
import java.util.List;

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
public class NoiseLink {

	private final Id<Link> id;	
	private List<Id<Vehicle>> enteringVehicleIds = new ArrayList<Id<Vehicle>>();
	private int carAgentsEntering = 0; // carAgents x scaleFactor = cars
	private int hgvAgentsEntering = 0; // hgvAgents x scaleFactor = hgv
	private int carAgentsLeaving = 0; 
	private int hgvAgentsLeaving = 0;
	private double travelTimeCar_Sec = 0.;
	private double travelTimeHGV_Sec = 0.;
	private double emission = 0.;
	
	private double emissionPlusOneCar = 0.;
	private double emissionPlusOneHGV = 0.;
	private double immissionPlusOneCar = 0.;
	private double immissionPlusOneHGV = 0.;
	
	private double damageCost = 0.; 
	private double averageDamageCostPerCar = 0.; 
	private double averageDamageCostPerHgv = 0.; 
	private double marginalDamageCostPerCar = 0.; 
	private double marginalDamageCostPerHgv = 0.; 
	
	public NoiseLink(Id<Link> linkId) {
		this.id = linkId;
	}
	public int getCarAgentsLeaving() {
		return carAgentsLeaving;
	}
	public void setCarAgentsLeaving(int carAgentsLeaving) {
		this.carAgentsLeaving = carAgentsLeaving;
	}
	public int getHgvAgentsLeaving() {
		return hgvAgentsLeaving;
	}
	public void setHgvAgentsLeaving(int hgvAgentsLeaving) {
		this.hgvAgentsLeaving = hgvAgentsLeaving;
	}
	public Id<Link> getId() {
		return id;
	}
	public List<Id<Vehicle>> getEnteringVehicleIds() {
		return enteringVehicleIds;
	}
	public void setEnteringVehicleIds(List<Id<Vehicle>> enteringVehicleIds) {
		this.enteringVehicleIds = enteringVehicleIds;
	}
	public int getCarAgentsEntering() {
		return carAgentsEntering;
	}
	public void setCarAgentsEntering(int carsEntering) {
		this.carAgentsEntering = carsEntering;
	}
	public int getHgvAgentsEntering() {
		return hgvAgentsEntering;
	}
	public void setHgvAgentsEntering(int hgvEntering) {
		this.hgvAgentsEntering = hgvEntering;
	}
	public double getDamageCost() {
		return damageCost;
	}
	public void setDamageCost(double damageCost) {
		this.damageCost = damageCost;
	}
	public double getAverageDamageCostPerCar() {
		return averageDamageCostPerCar;
	}
	public void setAverageDamageCostPerCar(double damageCostPerCar) {
		this.averageDamageCostPerCar = damageCostPerCar;
	}
	public double getAverageDamageCostPerHgv() {
		return averageDamageCostPerHgv;
	}
	public void setAverageDamageCostPerHgv(double damageCostPerHgv) {
		this.averageDamageCostPerHgv = damageCostPerHgv;
	}
	public double getEmission() {
		return emission;
	}
	public void setEmission(double emission) {
		this.emission = emission;
	}
	public double getEmissionPlusOneCar() {
		return emissionPlusOneCar;
	}
	public void setEmissionPlusOneCar(double emissionPlusOneCar) {
		this.emissionPlusOneCar = emissionPlusOneCar;
	}
	public double getEmissionPlusOneHGV() {
		return emissionPlusOneHGV;
	}
	public void setEmissionPlusOneHGV(double emissionPlusOneHGV) {
		this.emissionPlusOneHGV = emissionPlusOneHGV;
	}
	public double getImmissionPlusOneCar() {
		return immissionPlusOneCar;
	}
	public void setImmissionPlusOneCar(double immissionPlusOneCar) {
		this.immissionPlusOneCar = immissionPlusOneCar;
	}
	public double getImmissionPlusOneHGV() {
		return immissionPlusOneHGV;
	}
	public void setImmissionPlusOneHGV(double immissionPlusOneHGV) {
		this.immissionPlusOneHGV = immissionPlusOneHGV;
	}
	public double getMarginalDamageCostPerCar() {
		return marginalDamageCostPerCar;
	}
	public void setMarginalDamageCostPerCar(double marginalDamageCostPerCar) {
		this.marginalDamageCostPerCar = marginalDamageCostPerCar;
	}
	public double getMarginalDamageCostPerHgv() {
		return marginalDamageCostPerHgv;
	}
	public void setMarginalDamageCostPerHgv(double marginalDamageCostPerHgv) {
		this.marginalDamageCostPerHgv = marginalDamageCostPerHgv;
	}
	public double getTravelTimeCar_sec() {
		return travelTimeCar_Sec;
	}
	public void setTravelTimeCar_Sec(double travelTimeCar_Sec) {
		this.travelTimeCar_Sec = travelTimeCar_Sec;
	}
	public double getTravelTimeHGV_sec() {
		return travelTimeHGV_Sec;
	}
	public void setTravelTimeHGV_Sec(double travelTimeHGV_Sec) {
		this.travelTimeHGV_Sec = travelTimeHGV_Sec;
	}
	@Override
	public String toString() {
		return "NoiseLink [id=" + id + ", enteringVehicleIds="
				+ enteringVehicleIds + ", carAgents=" + carAgentsEntering
				+ ", hgvAgents=" + hgvAgentsEntering + ", emission=" + emission
				+ ", emissionPlusOneCar=" + emissionPlusOneCar
				+ ", emissionPlusOneHGV=" + emissionPlusOneHGV
				+ ", immissionPlusOneCar=" + immissionPlusOneCar
				+ ", immissionPlusOneHGV=" + immissionPlusOneHGV
				+ ", damageCost=" + damageCost + ", averageDamageCostPerCar="
				+ averageDamageCostPerCar + ", averageDamageCostPerHgv="
				+ averageDamageCostPerHgv + ", marginalDamageCostPerCar="
				+ marginalDamageCostPerCar + ", marginalDamageCostPerHgv="
				+ marginalDamageCostPerHgv + ", travelTimeCar="
				+ travelTimeCar_Sec + ", travelTimeHGV="
				+ travelTimeHGV_Sec + "]";
	}
	
	
}
