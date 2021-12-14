/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaColdEmissionFactorKey.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.emissions;

/**
 * @author benjamin
 * @author julia
 **/
class HbefaColdEmissionFactorKey extends HbefaEmissionFactorKey {

	private int parkingTime;
	private int distance;

	public HbefaColdEmissionFactorKey() {
		super();
	}

	public HbefaColdEmissionFactorKey(HbefaColdEmissionFactorKey key) {
		super(key);
		this.parkingTime = key.getParkingTime();
		this.distance = key.getDistance();
	}

	public int getParkingTime() {
		return parkingTime;
	}

	public void setParkingTime(int parkingTime) {
		this.parkingTime = parkingTime;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		var that = (HbefaColdEmissionFactorKey) o;

		if (parkingTime != that.getParkingTime()) return false;
		return distance == that.getDistance();
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + parkingTime;
		result = 31 * result + distance;
		return result;
	}


	// needed for "hashCode" method
	@Override
	public String toString(){
		return getVehicleCategory() + "; "
				+ getComponent() + "; "
				+ parkingTime + "; "
				+ distance + "; "
				+ getVehicleAttributes();
	}
}
