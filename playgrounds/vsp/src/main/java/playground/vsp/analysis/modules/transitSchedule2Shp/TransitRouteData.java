/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.transitSchedule2Shp;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Adds some specific transit route data
 * 
 * @author aneumann
 *
 */
public class TransitRouteData {

	private String transportMode;
	private double firstDeparture;
	private double lastDeparture;
	private Set<String> vehIds;
	private TransitStopFacility firstStop;
	private TransitStopFacility viaStop;
	private TransitStopFacility lastStop;
	private double distance;
	private double freeSpeedTravelTime;
	private double travelTime;
	private int numberOfDepartures;
	private double avgSpeed;

	public TransitRouteData(Network network, TransitRoute transitRoute) {

		this.transportMode = transitRoute.getTransportMode();

		this.firstDeparture = Double.MAX_VALUE;
		this.lastDeparture = -Double.MAX_VALUE;
		this.vehIds = new TreeSet<String>();
		this.numberOfDepartures = 0;
		
		for (Departure departure : transitRoute.getDepartures().values()) {
			this.firstDeparture = Math.min(this.firstDeparture,	departure.getDepartureTime());
			this.lastDeparture = Math.max(this.lastDeparture, departure.getDepartureTime());
			this.vehIds.add(departure.getVehicleId().toString());
			this.numberOfDepartures++;
		}

		this.firstStop = transitRoute.getStops().get(0).getStopFacility();
		this.lastStop = transitRoute.getStops().get(transitRoute.getStops().size() - 1).getStopFacility();

		if (this.firstStop == this.lastStop) {
			// get the stop location of stop with the largest distance between first and last stop
			TransitStopFacility currentViaStop = null;
			double currentViaDistance = Double.NEGATIVE_INFINITY;
			for (TransitRouteStop stop : transitRoute.getStops()) {
				double distanceFirstPotentialVia = CoordUtils.calcEuclideanDistance(this.firstStop.getCoord(), stop.getStopFacility().getCoord());
				double distanceLastProtenialVia = CoordUtils.calcEuclideanDistance(this.lastStop.getCoord(), stop.getStopFacility().getCoord());
				double newDistance = Math.sqrt(Math.pow(distanceFirstPotentialVia, 2) + Math.pow(distanceLastProtenialVia, 2));

				if (newDistance > currentViaDistance) {
					// this one is farther away - keep it
					currentViaStop = stop.getStopFacility();
					currentViaDistance = newDistance;
				}
			}
			this.viaStop = currentViaStop;
		} else {
			// get the stop in the middle of the line
			this.viaStop = transitRoute.getStops().get((int) (transitRoute.getStops().size() / 2)).getStopFacility();
		}

		// calculate the length of the route
		double distance = 0.0;
		double freeSpeedTravelTime = 0.0;
		for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
			Link link = network.getLinks().get(linkId);
			distance += link.getLength();
			freeSpeedTravelTime += link.getLength() / link.getFreespeed();
		}
		// add last link but not the first link
		Link link = network.getLinks().get(transitRoute.getRoute().getEndLinkId());
		distance += link.getLength();
		freeSpeedTravelTime += link.getLength() / link.getFreespeed();
		
		this.distance = distance;
		this.freeSpeedTravelTime = freeSpeedTravelTime;

		this.travelTime = transitRoute.getStops().get(transitRoute.getStops().size() - 1).getArrivalOffset();

		this.avgSpeed = this.distance / this.travelTime;
	}

	public String getTransportMode() {
		return this.transportMode;
	}

	public double getFirstDeparture() {
		return this.firstDeparture;
	}

	public double getLastDeparture() {
		return this.lastDeparture;
	}

	public int getNVehicles() {
		return this.vehIds.size();
	}

	public String getFirstStopName() {
		if (this.firstStop.getName() == null) {
			return this.firstStop.getId().toString();
		}
		return this.firstStop.getName();
	}

	public String getViaStopName() {
		if (this.viaStop.getName() == null) {
			return this.viaStop.getId().toString();
		}
		return this.viaStop.getName();
	}

	public String getLastStopName() {
		if (this.lastStop.getName() == null) {
			return this.lastStop.getId().toString();
		}
		return this.lastStop.getName();
	}

	public double getDistance() {
		return this.distance;
	}

	public double getFreeSpeedTravelTime() {
		return this.freeSpeedTravelTime;
	}

	public double getTravelTime() {
		return this.travelTime;
	}

	public double getHeadway() {
		if (this.numberOfDepartures == 1) {
			return Double.NaN;
		} else {
			return (this.getLastDeparture() - this.getFirstDeparture()) / (this.numberOfDepartures - 1);
		}
	}
	
	public int getNDepartures() {
		return this.numberOfDepartures;
	}

	public double getAvgSpeed() {
		return this.avgSpeed;
	}
	
	public double getFreeSpeedFactor() {
		return this.getFreeSpeedTravelTime() / this.getTravelTime();
	}
	
	public double getOperatingDuration() {
		return this.getLastDeparture() + this.getTravelTime() - this.getFirstDeparture();
	}
}
