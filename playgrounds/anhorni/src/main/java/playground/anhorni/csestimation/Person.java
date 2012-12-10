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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Person {
	private Id id;
	private int age = -99;
	private String sex;
	private int hhIncome = -99;
	private int hhSize = -99;
	private int nbrPersonShoppingTripsMonth = -99;
	private String nbrShoppingTripsMonth = "-99";		
		
	// ----------------------------------------------------
	private boolean isWorker;		
	private int areaToShop[] = new int[4]; 	
	private ArrayList<Coord> workRoute = new ArrayList<Coord>();
	// ----------------------------------------------------
	
	private PersonLocations locations = new PersonLocations();
	private PersonModes modes = new PersonModes();
	
	public Person(Id id) {
		this.id = id;
	}
	
	public void setHomeLocation(Location location) {
		this.locations.setHomeLocation(location);
	}	
	public void setWorkLocation(Location location) {
		this.locations.setWorkLocation(location);
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
	public Id getId() {
		return id;
	}
	public int getAge() {
		return age;
	}
	public String sex() {
		return sex;
	}
	public int getHhIncome() {
		return hhIncome;
	}
	public int getHhSize() {
		return hhSize;
	}
	public int getNbrPersonShoppingTripsMonth() {
		return nbrPersonShoppingTripsMonth;
	}
	public String getNbrShoppingTripsMonth() {
		return nbrShoppingTripsMonth;
	}
	public boolean isWorker() {
		return isWorker;
	}
	public int[] getAreaToShop() {
		return areaToShop;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public void setSex(String sex) {
		this.sex = sex;
	}
	public void setHhIncome(int hhIncome) {
		this.hhIncome = hhIncome;
	}
	public void setHhSize(int hhSize) {
		this.hhSize = hhSize;
	}
	public void setNbrShoppingTripsMonth(String nbrShoppingTripsMonth) {
		this.nbrShoppingTripsMonth = nbrShoppingTripsMonth;
	}
	public void setNbrPersonShoppingTripsMonth(int nbrPersonShoppingTripsMonth) {
		this.nbrPersonShoppingTripsMonth = nbrPersonShoppingTripsMonth;
	}

	public void setWorker(boolean isWorker) {
		this.isWorker = isWorker;
	}
	public void setAreaToShop(int[] areaToShop) {
		this.areaToShop = areaToShop;
	}
}
