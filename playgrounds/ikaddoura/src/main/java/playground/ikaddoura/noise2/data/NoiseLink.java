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
	private int cars = 0;
	private int hgv = 0;
	private double emission = 0.;
	private double damageCost = 0.;
	private double damageCostPerCar = 0.;
	private double damageCostPerHgv = 0.;
	
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
	public int getCars() {
		return cars;
	}
	public void setCars(int cars) {
		this.cars = cars;
	}
	public int getHgv() {
		return hgv;
	}
	public void setHgv(int hgv) {
		this.hgv = hgv;
	}
	public double getDamageCost() {
		return damageCost;
	}
	public void setDamageCost(double damageCost) {
		this.damageCost = damageCost;
	}
	public double getDamageCostPerCar() {
		return damageCostPerCar;
	}
	public void setDamageCostPerCar(double damageCostPerCar) {
		this.damageCostPerCar = damageCostPerCar;
	}
	public double getDamageCostPerHgv() {
		return damageCostPerHgv;
	}
	public void setDamageCostPerHgv(double damageCostPerHgv) {
		this.damageCostPerHgv = damageCostPerHgv;
	}
	public double getEmission() {
		return emission;
	}
	public void setEmission(double emission) {
		this.emission = emission;
	}
	
	
}
