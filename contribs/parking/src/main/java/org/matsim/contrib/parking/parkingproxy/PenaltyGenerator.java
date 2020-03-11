/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

/**
 * A PenaltyGenerator acts as a Factory class for {@linkplain PenaltyCalculator}s. It does so
 * by taking any input either all at once or by collecting it over a longer time and on demand
 * builds a new instance of {@linkplain PenaltyCalculator} with the collected information.
 * 
 * @author tkohl / Senozon
 *
 */
public interface PenaltyGenerator {

	/**
	 * Generates an (almost) immutable {@linkplain PenaltyCalculator} based on the current state
	 * of this class. Further data collected by this class will not change the results of already
	 * generated {@linkplain PenaltyCalculators}. However, you may later plug in your own
	 * {@linkplain PenaltyFunction}.
	 * 
	 * @return the generated immutable calculator instance
	 */
	public PenaltyCalculator generatePenaltyCalculator();
	
	/**
	 * Resets the state of this class.
	 */
	public void reset();
}
