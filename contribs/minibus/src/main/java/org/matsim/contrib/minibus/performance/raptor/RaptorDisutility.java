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

package org.matsim.contrib.minibus.performance.raptor;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.*;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author aneumann modified version of {@link TransitRouterNetworkTravelTimeAndDisutility}
 *
 */
public class RaptorDisutility implements TransitTravelDisutility {
	
	final static double MIDNIGHT = 24.0*3600;
	
	private final TransitRouterConfig config;
	private final double costPerBoarding;
	private final double costPerMeterTraveled;
	
	public RaptorDisutility(final TransitRouterConfig config, double costPerBoarding, double costPerMeterTraveled) {
		this.config = config;
		this.costPerBoarding = costPerBoarding;
		this.costPerMeterTraveled = costPerMeterTraveled;
	}
	
	/**
	 * Note that there is no off vehicle wait time.
	 */
	protected double getInVehicleTravelDisutility(final RouteSegment routeSegment) {
		double cost = 0.0;
		
		// this assumes dwell time as in-vehicle time
		double inVehicleTravelTime = routeSegment.getTravelTime();
		double inVehicleBeelineDistance = CoordUtils.calcEuclideanDistance(routeSegment.getFromStop().getCoord(), routeSegment.getToStop().getCoord());
		
		cost += - inVehicleTravelTime * this.config.getMarginalUtilityOfTravelTimePt_utl_s();
		cost += - inVehicleBeelineDistance * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		
		// fare
		cost += this.costPerBoarding;
		cost += inVehicleBeelineDistance * this.costPerMeterTraveled;
		
		return cost;
	}
	
	/**
	 * cost of a single transfer. Fare is included in {@link RaptorDisutility.getInVehicleTravelDisutility}
	 */
	protected final double getTransferCost(final Coord fromStop, final Coord toStop) {
		// this is same as defaultTransferCost in TransitRouterNetworkTravelTimeAndDisutility. Amit Oct'17
		double cost;
		
		double transfertime = getTransferTime(fromStop, toStop);
		double waittime = this.config.getAdditionalTransferTime();
		
		// say that the effective walk time is the transfer time minus some "buffer"
		double walktime = transfertime - waittime;

		if ( walktime < 0. ) {
			throw new RuntimeException( "negative walk time; should not happen; needs to be repaired" ) ;
		}
		
		double walkDistance = CoordUtils.calcEuclideanDistance(fromStop, toStop);
		
		// weigh this "buffer" not with the walk time disutility, but with the wait time disutility:
		// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
		// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
		// be the same as the marginal opprotunity cost of time).  kai, nov'11
		cost = - walktime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
		       - walkDistance * this.config.getMarginalUtilityOfTravelDistancePt_utl_m() 
		       - waittime * this.config.getMarginalUtilityOfWaitingPt_utl_s()
		       - this.config.getUtilityOfLineSwitch_utl();
		
		return cost;
	}

	@Override
	public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
		//  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
		double timeCost = - getTravelTime(coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s() ;
		// (sign: margUtl is negative; overall it should be positive because it is a cost.)
		
		double distanceCost = - CoordUtils.calcEuclideanDistance(coord,toCoord) * config.getMarginalUtilityOfTravelDistancePt_utl_m() ;
		// (sign: same as above)
		
		return timeCost + distanceCost ;
	}

	protected double getTransferTime(Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime + this.config.getAdditionalTransferTime();
	}
	
	protected double getTravelTime(Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, CustomDataManager dataManager) {
		throw new UnsupportedOperationException("not implemented yet.");
	}

	@Override
	public double getWalkTravelTime(Person person, Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime;
	}
}
