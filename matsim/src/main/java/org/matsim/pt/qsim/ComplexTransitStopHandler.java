/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt.qsim;

import java.util.List;

import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;

/**
 * @author aneumann
 */
public class ComplexTransitStopHandler implements TransitStopHandler {

	private boolean doorsOpen = false;
	private double passengersLeavingTimeFraction = 0.0;
	private double passengersEnteringTimeFraction = 0.0;

	private final double personEntersTime;
	private final double personLeavesTime;
	
	// TODO make it dynamic
	private static final double openDoorsDuration = 1.0;
	private static final double closeDoorsDuration = 1.0;

	public ComplexTransitStopHandler(BasicVehicle vehicle) {
		this.personEntersTime = vehicle.getType().getAccessTime();
		this.personLeavesTime = vehicle.getType().getEgressTime();
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PassengerAgent> leavingPassengers,
			List<PassengerAgent> enteringPassengers, PassengerAccessEgress handler) {
		double stopTime = 0.0;

		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();

		if (!this.doorsOpen) {
			// doors are closed

			if ((cntAccess > 0) || (cntEgress > 0)) {
				// case doors are shut, but passengers want to leave or enter
				// the veh
				this.doorsOpen = true;
				stopTime = openDoorsDuration; // Time to open doors
			} else {
				// case nobody wants to leave or enter the veh
				stopTime = 0.0;
			}

		} else {
			// doors are already open

			if ((cntAccess > 0) || (cntEgress > 0)) {
				// somebody wants to leave or enter the veh

				if (cntAccess > 0) {

					if (this.passengersEnteringTimeFraction < 1.0) {

						// next passenger can enter the veh

						while (this.passengersEnteringTimeFraction < 1.0) {
							if (enteringPassengers.size() == 0) {
								break;
							}

							handler.handlePassengerEntering(enteringPassengers.get(0), now);
							enteringPassengers.remove(0);
							this.passengersEnteringTimeFraction += personEntersTime;
						}

						this.passengersEnteringTimeFraction -= 1.0;
						stopTime = 1.0;

					} else {
						// still time needed to allow next passenger to enter
						this.passengersEnteringTimeFraction -= 1.0;
						stopTime = 1.0;
					}

				} else {
					this.passengersEnteringTimeFraction -= 1.0;
					this.passengersEnteringTimeFraction = Math.max(0, this.passengersEnteringTimeFraction);
				}

				if (cntEgress > 0) {

					if (this.passengersLeavingTimeFraction < 1.0) {
						// next passenger can leave the veh

						while (this.passengersLeavingTimeFraction < 1.0) {
							if (leavingPassengers.size() == 0) {
								break;
							}
							handler.handlePassengerLeaving(leavingPassengers.get(0), now);
							leavingPassengers.remove(0);
							this.passengersLeavingTimeFraction += personLeavesTime;
						}

						this.passengersLeavingTimeFraction -= 1.0;
						stopTime = 1.0;

					} else {
						// still time needed to allow next passenger to leave
						this.passengersLeavingTimeFraction -= 1.0;
						stopTime = 1.0;
					}

				} else {
					this.passengersLeavingTimeFraction -= 1.0;
					this.passengersLeavingTimeFraction = Math.max(0, this.passengersLeavingTimeFraction);
				}

			} else {

				// nobody left to handle

				if (this.passengersEnteringTimeFraction < 1.0 && this.passengersLeavingTimeFraction < 1.0) {
					// every passenger entered or left the veh so close and
					// leave

					this.doorsOpen = false;
					this.passengersEnteringTimeFraction = 0.0;
					this.passengersLeavingTimeFraction = 0.0;
					stopTime = closeDoorsDuration; // Time to shut the doors
				}

				// somebody is still leaving or entering the veh so wait again

				if (this.passengersEnteringTimeFraction >= 1) {
					this.passengersEnteringTimeFraction -= 1.0;
					stopTime = 1.0;
				}

				if (this.passengersLeavingTimeFraction >= 1) {
					this.passengersLeavingTimeFraction -= 1.0;
					stopTime = 1.0;
				}

			}

		}

		return stopTime;
	}

}
