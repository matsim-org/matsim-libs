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

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;

/**
 * @author  jbischoff
 *
 */

public interface TaxibusLine {

	public Id<TaxibusLine> getId();

	public Id<TaxibusLine> getReturnRouteId();
	
	public void setReturnRouteId(Id<TaxibusLine> id);
	
	/**
	 * @return Lambda currently applicable
	 */
	public double getCurrentLambda();
	
	
	
	/**
	 * @param time 
	 * @return Lambda for defined time
	 */
	public double getLambda(double time);
	
	public double getCurrentOccupationRate();
	
	
	/**
	 * @return the Link's id where busses are stowed prior to dispatch
	 */
	public Id<Link> getHoldingPosition();
	
	
	/**
	 * @return expected TravelTime for a Single Trip w/o drop offs or pick ups
	 */
	public double getSingleTripTravelTime();
	
	
	/**
	 * @return maximum time spent on collecting passengers after initial dispatch
	 */
	public double getCurrentTwMax();
	
	public void addVehicleToHold(Vehicle veh);
	
	public boolean removeVehicleFromHold(Vehicle veh);
	
	public Vehicle getNextEmptyVehicle();
	
	public boolean lineServesRequest(TaxibusRequest request);
	
	public boolean lineCoversCoordinate(Coord coord);

	public boolean isVehicleInHold();

	public int getMaximumOpenVehicles();

	public void reset();
	
	
}
