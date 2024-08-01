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
 * Uses a linear raise in penalty time per car but stays between 0 and a maximum value. In mathematical terms this means: </br>
 * {@code P = [ 0 if c<0; r*c if 0<=c<=c_m; m if c>c_m},</br>
 * where P is the penalty in seconds, c is the number of cars, r is the additional penalty per car, m is the maximum 
 * penalty and c_m is the number of cars at which r*c=m.
 * 
 * @author tkohl / Senozon
 *
 */
class LinearPenaltyFunctionWithCap implements PenaltyFunction {
	
	private final double penaltyPerCar;
	private final double maxPenalty;
	
	public LinearPenaltyFunctionWithCap(double penaltyPerCar, double maxPenalty) {
		this.penaltyPerCar = penaltyPerCar;
		this.maxPenalty = maxPenalty;
	}

	@Override
	public double calculatePenalty(int numberOfCars) {
		return Math.max(Math.min(numberOfCars * penaltyPerCar, maxPenalty), 0);
	}

}
