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

package playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;

/**
 * @author jbischoff
 *
 */
public class TaxibusLineImpl implements TaxibusLine {

	private final Id<TaxibusLine> lineId;
	private final Id<Link> holdingPosition;
	private final MultiPolygon departureZone;
	private final MultiPolygon arrivalZone;

	private Id<TaxibusLine> returnLineId;

	private Queue<Vehicle> vehiclesInHold = new LinkedList<>();

	private double currentLambda = 0.;
	private double currentTwMax;
	private double currentOccupationRate = 8;
	private int maxVehicles = 8;
	private double singleTripTravelTime;

	/**
	 * @param lineId the Line Id
	 * @param holdingPosition Link Id where taxibusses stand by for line dispatch
	 * @param departureZone Departure area
	 * @param arrivalZone Arrival Area
	 * @param twMax time waited between first pickup and departure towards destination in seconds
	 */
	public TaxibusLineImpl(Id<TaxibusLine> lineId, Id<Link> holdingPosition, MultiPolygon departureZone,
			MultiPolygon arrivalZone, double twMax) {
		this.lineId = lineId;
		this.holdingPosition = holdingPosition;
		this.departureZone = departureZone;
		this.arrivalZone = arrivalZone;
		this.currentTwMax = twMax;
	}

	@Override
	public Id<TaxibusLine> getId() {
		return lineId;
	}

	@Override
	public Id<TaxibusLine> getReturnRouteId() {
		return returnLineId;
	}

	@Override
	public void setReturnRouteId(Id<TaxibusLine> id) {
		this.returnLineId = id;
	}

	@Override
	public double getCurrentLambda() {
		return currentLambda;
	}

	@Override
	public double getLambda(double time) {
		// todo
		return 0;
	}

	@Override
	public double getCurrentOccupationRate() {

		return currentOccupationRate;
	}

	@Override
	public Id<Link> getHoldingPosition() {
		return holdingPosition;
	}

	@Override
	public double getSingleTripTravelTime() {
		return singleTripTravelTime;
	}

	@Override
	public double getCurrentTwMax() {
		return currentTwMax;
	}

	@Override
	public void addVehicleToHold(Vehicle veh) {
		this.vehiclesInHold.add(veh);
	}

	@Override
	public boolean removeVehicleFromHold(Vehicle veh) {
		return this.vehiclesInHold.remove(veh);
	}

	@Override
	public Vehicle getNextEmptyVehicle() {
		return this.vehiclesInHold.poll();
	}
	@Override
	public boolean isVehicleInHold(){
		return (!this.vehiclesInHold.isEmpty());
	}

	@Override
	public boolean lineServesRequest(TaxibusRequest request) {

		return (departureZone.contains(MGC.coord2Point(request.getFromLink().getCoord()))
				&& arrivalZone.contains(MGC.coord2Point(request.getToLink().getCoord())));
	}

	@Override
	public boolean lineCoversCoordinate(Coord coord) {
		Geometry g = MGC.coord2Point(coord);
		return (departureZone.contains(g) || arrivalZone.contains(g));
	}

	@Override
	public int getMaximumOpenVehicles() {

		return maxVehicles;
	}

	@Override
	public void reset() {
		this.vehiclesInHold.clear();
	}

}
