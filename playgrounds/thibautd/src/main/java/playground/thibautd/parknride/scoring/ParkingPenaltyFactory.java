/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingPenaltyFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.scoring;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author thibautd
 */
public interface ParkingPenaltyFactory {
	/**
	 * Creates a new instance
	 * @param plan the plan which variants are to score: can be used
	 * to obtain information about the person or whatever.
	 * @return a new instance
	 */
	public ParkingPenalty createPenalty(final Plan plan);
} 
