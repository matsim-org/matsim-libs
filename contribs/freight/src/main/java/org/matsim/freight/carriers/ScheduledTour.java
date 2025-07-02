/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;


/**
 * A concrete assignment of a tour, a vehicle and a departure time.
 *
 * @author sschroeder, mzilske
 *
 */
public class ScheduledTour {


	/**
	 * Returns a new instance of ScheduledTour.
	 *
	 * <p>Look at the builder. It might be easier to build a scheduled tour.
	 * You get the builder this way: ScheduledTour.Builder.newInstance(carrierVehicle).
	 *
	 * @param tour				The scheduled tour.
	 * @param vehicle			The vehicle for the tour.
	 * @param departureTime 	The time when the vehicle starts the tour.
	 * @return a scheduledTour
	 * @see ScheduledTour
	 */
	public static ScheduledTour newInstance(Tour tour, CarrierVehicle vehicle, double departureTime){
		return new ScheduledTour(tour,vehicle,departureTime);
	}

	private final Tour tour;

	private final CarrierVehicle vehicle;

	private final double departureTime;

	private ScheduledTour(final Tour tour, final CarrierVehicle vehicle, final double departureTime) {
		this.tour = tour;
		this.vehicle = vehicle;
		this.departureTime = departureTime;
	}

	public Tour getTour() {
		return tour;
	}

	public CarrierVehicle getVehicle() {
		return vehicle;
	}

	public double getDeparture() {
		return departureTime;
	}

	@Override
	public String toString() {
		return "scheduledTour=[tour="+tour+"][vehicle="+vehicle+"][departureTime="+departureTime+"]";
	}

}
