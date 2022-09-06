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

package org.matsim.core.mobsim.qsim.pt;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Continuous queue model like transit stop handler using either serial or parallel doors operation mode.
 * 
 * 
 * @author aneumann
 */
public class ComplexTransitStopHandler implements TransitStopHandler {
	
	private final static Logger log = LogManager.getLogger(ComplexTransitStopHandler.class);

	private boolean doorsOpen = false;
	private double passengersLeavingTimeFraction = 0.0;
	private double passengersEnteringTimeFraction = 0.0;

	private final double personEntersTime;
	private final double personLeavesTime;
	private final VehicleType.DoorOperationMode doorOperationMode;
	
	// TODO make it dynamic
	private static final double openDoorsDuration = 1.0;
	private static final double closeDoorsDuration = 1.0;

	/*package*/ ComplexTransitStopHandler(Vehicle vehicle) {
        this.personEntersTime = VehicleUtils.getAccessTime(vehicle.getType());
		this.personLeavesTime = VehicleUtils.getEgressTime(vehicle.getType());
		this.doorOperationMode = VehicleUtils.getDoorOperationMode(vehicle.getType());
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers,
			List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle) {
		
		if(this.doorOperationMode == VehicleType.DoorOperationMode.parallel){
			return handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);			
		} else if (this.doorOperationMode == VehicleType.DoorOperationMode.serial){
			return handleSerialStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		} else {
			log.info("Unimplemented door operation mode " + this.doorOperationMode + " set. Using parralel mode as default.");
			return handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		}		
	}
	
	private double handleSerialStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers, 
			List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle){
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
				
				if (cntEgress > 0) {

					if (this.passengersLeavingTimeFraction < 1.0) {
						// next passenger can leave the veh

						while (this.passengersLeavingTimeFraction < 1.0) {
							if (leavingPassengers.size() == 0) {
								break;
							}

							if(handler.handlePassengerLeaving(leavingPassengers.get(0), vehicle, stop.getLinkId(), now)){
								leavingPassengers.remove(0);
								this.passengersLeavingTimeFraction += personLeavesTime;
							} else {
								break;
							}

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
					
					if (cntAccess > 0) {

						if (this.passengersEnteringTimeFraction < 1.0) {

							// next passenger can enter the veh

							while (this.passengersEnteringTimeFraction < 1.0) {
								if (enteringPassengers.size() == 0) {
									break;
								}

								if(handler.handlePassengerEntering(enteringPassengers.get(0), vehicle, stop.getId(), now)){
									enteringPassengers.remove(0);
									this.passengersEnteringTimeFraction += personEntersTime;
								} else {
									break;
								}

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
	
	private double handleParallelStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers,
			List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle){
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

							if(handler.handlePassengerEntering(enteringPassengers.get(0), vehicle, stop.getId(), now)){
								enteringPassengers.remove(0);
								this.passengersEnteringTimeFraction += personEntersTime;
							} else {
								break;
							}

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

							if(handler.handlePassengerLeaving(leavingPassengers.get(0), vehicle, stop.getLinkId(), now)){
								leavingPassengers.remove(0);
								this.passengersLeavingTimeFraction += personLeavesTime;
							} else {
								break;
							}

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
