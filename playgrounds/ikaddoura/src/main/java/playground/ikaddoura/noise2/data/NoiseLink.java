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
package playground.ikaddoura.noise2.data;

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
	private int carAgents = 0; // carAgents x scaleFactor = cars
	private int hgvAgents = 0; // hgvAgents x scaleFactor = hgv
	private double emission = 0.;
	
	private double emissionMinusOneCar = 0.;
	private double emissionMinusOneHGV = 0.;
	private double immissionMinusOneCar = 0.;
	private double immissionMinusOneHGV = 0.;
	
	private double damageCost = 0.; 
	private double averageDamageCostPerCar = 0.; 
	private double averageDamageCostPerHgv = 0.; 
	private double marginalDamageCostPerCar = 0.; 
	private double marginalDamageCostPerHgv = 0.; 
	
	public NoiseLink(Id<Link> linkId) {
		this.id = linkId;
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
	public int getCarAgents() {
		return carAgents;
	}
	public void setCarAgents(int cars) {
		this.carAgents = cars;
	}
	public int getHgvAgents() {
		return hgvAgents;
	}
	public void setHgvAgents(int hgv) {
		this.hgvAgents = hgv;
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
	public double getEmissionMinusOneCar() {
		return emissionMinusOneCar;
	}
	public void setEmissionMinusOneCar(double emissionMinusOneCar) {
		this.emissionMinusOneCar = emissionMinusOneCar;
	}
	public double getEmissionMinusOneHGV() {
		return emissionMinusOneHGV;
	}
	public void setEmissionMinusOneHGV(double emissionMinusOneHGV) {
		this.emissionMinusOneHGV = emissionMinusOneHGV;
	}
	public double getImmissionMinusOneCar() {
		return immissionMinusOneCar;
	}
	public void setImmissionMinusOneCar(double immissionMinusOneCar) {
		this.immissionMinusOneCar = immissionMinusOneCar;
	}
	public double getImmissionMinusOneHGV() {
		return immissionMinusOneHGV;
	}
	public void setImmissionMinusOneHGV(double immissionMinusOneHGV) {
		this.immissionMinusOneHGV = immissionMinusOneHGV;
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
	@Override
	public String toString() {
		return "NoiseLink [id=" + id + ", enteringVehicleIds="
				+ enteringVehicleIds + ", carAgents=" + carAgents
				+ ", hgvAgents=" + hgvAgents + ", emission=" + emission
				+ ", emissionMinusOneCar=" + emissionMinusOneCar
				+ ", emissionMinusOneHGV=" + emissionMinusOneHGV
				+ ", immissionMinusOneCar=" + immissionMinusOneCar
				+ ", immissionMinusOneHGV=" + immissionMinusOneHGV
				+ ", damageCost=" + damageCost + ", averageDamageCostPerCar="
				+ averageDamageCostPerCar + ", averageDamageCostPerHgv="
				+ averageDamageCostPerHgv + ", marginalDamageCostPerCar="
				+ marginalDamageCostPerCar + ", marginalDamageCostPerHgv="
				+ marginalDamageCostPerHgv + "]";
	}
}
