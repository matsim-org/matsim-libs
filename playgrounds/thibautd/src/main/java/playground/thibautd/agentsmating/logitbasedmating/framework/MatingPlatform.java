/* *********************************************************************** *
 * project: org.matsim.*
 * MatingPlatform.java
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

/**
 * Class responsible for mating together driver and passenger
 * requests.
 *
 * @author thibautd
 */
public abstract class MatingPlatform {
	/**
	 * Makes the platform aware of the request for further processing.
	 */
	public abstract void handleRequest(TripRequest request);

	/**
	 * Makes the platform aware of the requests for further processing.
	 */
	public void handleRequests(final List<TripRequest> requests) {
		for (TripRequest request : requests) {
			handleRequest(request);
		}
	}

	/**
	 * (Computes and) returns the matings for the notified requests
	 */
	public abstract List<Mating> getMatings();
}

