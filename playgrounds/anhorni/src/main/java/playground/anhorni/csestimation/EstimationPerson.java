/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.csestimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

import playground.anhorni.analysis.microcensus.planbased.MZPerson;

public class EstimationPerson extends MZPerson {
	private int nbrPersonShoppingTripsMonth = -99;
	private String nbrShoppingTripsMonth = "-99";		
		
	// ----------------------------------------------------	
	private int areaToShop[] = new int[4]; 	
	private ArrayList<Coord> workRoute = new ArrayList<Coord>();
	// ----------------------------------------------------
	
	private PersonLocations locations = new PersonLocations();
	private PersonModes modes = new PersonModes();
	private HomeSet homeset;
	
	private List<ShoppingTrip> shoppingTrips = new Vector<ShoppingTrip>();
	private final static Logger log = Logger.getLogger(EstimationPerson.class);
	
	public EstimationPerson(Id id) {
		super(id);
	}	
	
	public EstimationPerson(MZPerson mzPerson) {
		super(mzPerson.getId());
		super.setAge(mzPerson.getAge());
		super.setCarAvail(mzPerson.getCarAvail());
		super.setDay(mzPerson.getDay());
		super.setEmployed(mzPerson.isEmployed());
		super.setHh(mzPerson.getHh());
		super.setHhIncome(mzPerson.getHhIncome());
		super.setHhSize(mzPerson.getHhSize());
		super.setPlz(mzPerson.getPlz());
		super.addPlan(mzPerson.getSelectedPlan());
		super.setSelectedPlan(mzPerson.getSelectedPlan());
		super.setLicence(mzPerson.getLicense());
		super.setSex(mzPerson.getSex());
		super.setWeight(mzPerson.getWeight());
	}
	
	public void setHomeLocation(Location location) {
		this.locations.setHomeLocation(location);
	}	
	public void setWorkLocation(Location location) {
		this.locations.setWorkLocation(location);
	}
	public void addStore(ShopLocation store, int aware, int visited)  {
		this.locations.addStore(store, aware, visited);
	}
	public void addNullStore(ShopLocation store) {
		this.locations.addNullStore(store);
	}
	public Location getHomeLocation() {
		return this.locations.getHomeLocation();
	}
	public void setModeForWorking(int index, boolean value) {
		this.modes.setModeForWorking(index, value);
	}
	public void setModesForShopping(int index, int value) {
		this.modes.setModesForShopping(index, value);
	}
	public int[] getModesForShopping() {
		return this.modes.getModesForShopping();
	}
	public boolean[] getModesForWorking() {
		return this.modes.getModesForWorking();
	}
	public void addWayPoint(Coord wayPoint) {
		this.workRoute.add(wayPoint);
	}
	public ArrayList<Coord> getWorkRoute() {
		return workRoute;
	}
	public void setAreaToShop(int index, int value) {
		this.areaToShop[index] = value;
	}	
	public int getNbrPersonShoppingTripsMonth() {
		return nbrPersonShoppingTripsMonth;
	}
	public String getNbrShoppingTripsMonth() {
		return nbrShoppingTripsMonth;
	}
	public int[] getAreaToShop() {
		return areaToShop;
	}
	public void setNbrShoppingTripsMonth(String nbrShoppingTripsMonth) {
		this.nbrShoppingTripsMonth = nbrShoppingTripsMonth;
	}
	public void setNbrPersonShoppingTripsMonth(int nbrPersonShoppingTripsMonth) {
		this.nbrPersonShoppingTripsMonth = nbrPersonShoppingTripsMonth;
	}
	public void setAreaToShop(int[] areaToShop) {
		this.areaToShop = areaToShop;
	}
	public void addShoppingTrip(ShoppingTrip t) {
		this.shoppingTrips.add(t);
	}
	public List<ShoppingTrip> getShoppingTrips() {
		return this.shoppingTrips;
	}
	public HomeSet getHomeset() {
		return homeset;
	}
	public void setHomeset(HomeSet homeset) {
		this.homeset = homeset;
	}
	public void createHomeSet(QuadTree<Location> shopQuadTree) {
		this.homeset = new HomeSet();
		this.homeset.create(this, shopQuadTree);
	}
	public PersonLocations getPersonLocations() {
		return this.locations;
	}
}
