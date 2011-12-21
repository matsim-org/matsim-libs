/* *********************************************************************** *
 * project: org.matsim.*
 * TripRequest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;

/**
 * Represents a car-pooling trip "request", that is, a choice of the car-pooling
 * mode before a mate (passenger or driver) has been identified.
 *
 * A shared ride alternative must provide mandatory attributes, specified by the getters
 *
 * @author thibautd
 */
public interface TripRequest extends Alternative {
	/**
	 * @return the type of trip
	 */
	public Type getTripType();

	/**
	 * @return a list containing the other mode alternatives (eg car, pt...) for
	 * this trip.
	 */
	public List<Alternative> getAlternatives();

	/**
	 * @return the decision maker associated with this request
	 */
	public DecisionMaker getDecisionMaker();

	/**
	 * @return the index of the corresponding leg in the individual plan.
	 */
	public int getIndexInPlan();

	//public Id getOriginLinkId();
	//public Id getDestinationLinkId();
	public double getDepartureTime();
	public double getPlanArrivalTime();
	/**
	 * @return the origin activity, as it appears in the input plan
	 */
	public Activity getOrigin();
	/**
	 * @return the destination activity, as it appears in the input plan
	 */
	public Activity getDestination();

	public enum Type {
		DRIVER,
		PASSENGER
	}
}

