/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.passenger;

import java.util.Optional;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface DrtOfferAcceptor {
	/**
	 * @param request       drt request
	 * @param departureTime offered departure time for the new request
	 * @param arrivalTime   offered arrival time for the new request
	 * @return accepted request (if accepted) or empty
	 */
	Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime);
}
