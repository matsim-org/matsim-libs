/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package analysis.drtOccupancy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class DynModeTrip implements Comparable<DynModeTrip> {
	private final double departureTime;
	private final Id<Person> person;
	private final Id<Vehicle> vehicle;
	private final Id<Link> fromLinkId;
	private final double waitTime;
	private double travelTime = Double.NaN;
	private double travelDistance_m = Double.NaN;
	private double unsharedDistanceEstimate_m = Double.NaN;
	private double unsharedTimeEstimate_m = Double.NaN;
	private Id<Link> toLink = null;
	private double arrivalTime = Double.NaN;
	private final Coord fromCoord;
	private Coord toCoord = null;
	private final	DecimalFormat format;


	static final String demitter = ";";
	public static final String HEADER = "departureTime" + demitter + "personId" + demitter + "vehicleId" + demitter
			+ "fromLinkId" + demitter + "fromX" + demitter + "fromY" + demitter + "toLinkId" + demitter + "toX"
			+ demitter + "toY" + demitter + "waitTime" + demitter + "arrivalTime" + demitter + "travelTime" + demitter
			+ "travelDistance_m"+demitter+"direcTravelDistance_m";

	DynModeTrip(double departureTime, Id<Person> person, Id<Vehicle> vehicle, Id<Link> fromLinkId, Coord fromCoord,
			double waitTime) {
		this.departureTime = departureTime;
		this.person = person;
		this.vehicle = vehicle;
		this.fromLinkId = fromLinkId;
		this.fromCoord = fromCoord;
		this.waitTime = waitTime;
		
		this.format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	public Double getDepartureTime() {
		return departureTime;
	}

	public Id<Person> getPerson() {
		return person;
	}
	

	public void setUnsharedDistanceEstimate_m(double unsharedDistanceEstimate_m) {
		this.unsharedDistanceEstimate_m = unsharedDistanceEstimate_m;
	}

	public void setUnsharedTimeEstimate_m(double unsharedTimeEstimate_m) {
		this.unsharedTimeEstimate_m = unsharedTimeEstimate_m;
	}

	public Id<Vehicle> getVehicle() {
		return vehicle;
	}

	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	public double getWaitTime() {
		return waitTime;
	}

	public double getInVehicleTravelTime() {
		return travelTime;
	}

	public void setInVehicleTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public double getTravelDistance() {
		return travelDistance_m;
	}
	

	public double getUnsharedDistanceEstimate_m() {
		return unsharedDistanceEstimate_m;
	}
	
	public double getUnsharedTimeEstimate_m() {
		return unsharedTimeEstimate_m;
	}

	public void setTravelDistance(double travelDistance_m) {
		this.travelDistance_m = travelDistance_m;
	}

	public Id<Link> getToLinkId() {
		return toLink;
	}

	public void setToLink(Id<Link> toLink) {
		this.toLink = toLink;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public Coord getToCoord() {
		return toCoord;
	}

	public void setToCoord(Coord toCoord) {
		this.toCoord = toCoord;
	}

	public Coord getFromCoord() {
		return fromCoord;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DynModeTrip o) {
		return getDepartureTime().compareTo(o.getDepartureTime());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		double fromCoordX = Double.NaN;
		double fromCoordY = Double.NaN;

		double toCoordX = Double.NaN;
		double toCoordY = Double.NaN;
		if (toCoord != null) {
			toCoordX = toCoord.getX();
			toCoordY = toCoord.getY();
		}
		if (fromCoord != null) {
			fromCoordX = fromCoord.getX();
			fromCoordY = fromCoord.getY();
		}
		return getDepartureTime() + demitter + getPerson() + demitter + getVehicle() + demitter + getFromLinkId()
				+ demitter + format.format(fromCoordX) + demitter + format.format(fromCoordY) + demitter + getToLinkId() + demitter + format.format(toCoordX)
				+ demitter + format.format(toCoordY) + demitter + getWaitTime() + demitter + getArrivalTime() + demitter
				+ getInVehicleTravelTime() + demitter + format.format(getTravelDistance())+ demitter+ format.format(unsharedDistanceEstimate_m);
	}

}