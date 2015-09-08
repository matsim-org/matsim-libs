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

package playground.wrashid.parkingSearch.withindayFW.zhCity;


import playground.wrashid.lib.obj.Coord;

public class CityZone {

	private playground.wrashid.lib.obj.Coord zoneCentreCoord;
	private String id;
	private String name;
	private double pctBlueParking;
	private double pctNonFreeParking;
	private int zoneTariffType;
	private double parkingGarageFee2h;

	public void setId(String string) {
		this.id=string;
		
	}

	public void setName(String string) {
		this.name=string;
	}

	public void setPctBlueParking(double d) {
		this.pctBlueParking=d;
	}

	public void setPctNonFreeParking(double d) {
		this.pctNonFreeParking=d;
	}

	public void setZoneTariffType(int integer) {
		this.zoneTariffType=integer;
	}

	public void setParkingGarageFee2h(double double1) {
		this.parkingGarageFee2h=double1;
	}

	public Coord getZoneCentreCoord() {
		return zoneCentreCoord;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getPctBlueParking() {
		return pctBlueParking;
	}

	public double getPctNonFreeParking() {
		return pctNonFreeParking;
	}

	public int getZoneTariffType() {
		return zoneTariffType;
	}

	public double getParkingGarageFee2h() {
		return parkingGarageFee2h;
	}

	public void setZoneCentreCoord(double x, double y) {
		this.zoneCentreCoord= new Coord(x, y);
	}

	
	
}
