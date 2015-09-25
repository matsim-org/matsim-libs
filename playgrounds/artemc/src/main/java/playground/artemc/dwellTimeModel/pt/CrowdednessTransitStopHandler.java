package playground.artemc.dwellTimeModel.pt;

import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.PassengerAccessEgress;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * The dwell time of the parallel doors operation mode (=> Buses) has been modified from the initial MATSim code.
 * The Sun's Modell (2013) is the new model implemented
 * 
 *  @author sergioo, grerat
 * 
 */

public class CrowdednessTransitStopHandler implements TransitStopHandler {

	//Constants
	private final static Logger log = Logger.getLogger(ComplexTransitStopHandler.class);
	private static final double openDoorsDuration = 1.0;
	private static final double closeDoorsDuration = 1.0;
	private static final double ACC_DEC_DELAY = 8.0;


	//Attributes
	private TransitStopFacility lastHandledStop = new TransitScheduleFactoryImpl().createTransitStopFacility(Id.create("", TransitStopFacility.class), null, true);

	//Attributes
	private boolean doorsOpen = false;
	private double passengersLeavingTimeFraction = 0.0;
	private double passengersEnteringTimeFraction = 0.0;
	
	private double criticalOccupancy = 55.98; //Mercedez-Benz OC500LE
	
	//Constructor
	public CrowdednessTransitStopHandler(Vehicle vehicle){
	}
		
	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers,
			List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle) {
		if(vehicle.getVehicle().getType().getDoorOperationMode() == VehicleType.DoorOperationMode.parallel){			
			double dwellTime=handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
			return dwellTime;
		} else if (vehicle.getVehicle().getType().getDoorOperationMode() == VehicleType.DoorOperationMode.serial){
			return handleSerialStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		} else {
			log.info("Unimplemented door operation mode " + vehicle.getVehicle().getType().getDoorOperationMode() + " set. Using parralel mode as default.");
			return handleParallelStop(stop, now, leavingPassengers, enteringPassengers, handler, vehicle);
		}
	}
	
	private double handleParallelStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers, List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress handler, MobsimVehicle vehicle){
		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();
		double stopTime = 0;
		if (((cntAccess > 0) || (cntEgress > 0)) && this.lastHandledStop != null) {
			
			// The bus arrive at station (deceleration = 4s), the doors open (1s) and passengers boards and alights
			if (this.lastHandledStop != stop) {
				stopTime = getStopTimeParallel(leavingPassengers.size(), enteringPassengers.size(), vehicle) + openDoorsDuration+ACC_DEC_DELAY/2; // add fixed amount of time for door-opening and deceleration of the bus
			}
			
			// Boarding/alighting of the agents who came at facility during the boarding of the
			// first ones ("second wave"). The on-board passengers are not taken in account (see below)
			else {
				stopTime = getStopTimeParallelWithoutOccupancyEffect(leavingPassengers.size(), enteringPassengers.size(), vehicle);
			}
			
			for (PTPassengerAgent passenger : leavingPassengers) {
				handler.handlePassengerLeaving(passenger,vehicle, stop.getLinkId(), now);
			}
			for (PTPassengerAgent passenger : enteringPassengers) {
				handler.handlePassengerEntering(passenger,vehicle,stop.getId(), now);
			}
			this.lastHandledStop=stop;	
		}
		
		// The bus close the doors and leave to next station
		else if(this.lastHandledStop==stop){
			this.lastHandledStop=null;
			stopTime += closeDoorsDuration+ACC_DEC_DELAY/2;
		}
		else {
			this.lastHandledStop=stop;
		}
		
		return stopTime;
	}
	
	// Formula of Dwell Time model III, according to L. Sun (2013), with standard deviation 
	private double getStopTimeParallel(double leaving, double entering, MobsimVehicle vehicle) {
		double beta1 = vehicle.getVehicle().getType().getAccessTime();
		double beta2 = vehicle.getVehicle().getType().getEgressTime();
		double beta3 = criticalOccupancy;
		double mean = Math.max(beta1*(entering-1)+beta2*Math.max(vehicle.getPassengers().size()-beta3, 0), beta2*(leaving-1));	
		if(mean==0)
			return 0;
		double std = (2.2+0.12*mean); 
		try {
			double r = MatsimRandom.getRandom().nextDouble();
			return Math.max(new NormalDistributionImpl(mean,std).inverseCumulativeProbability(r), 0.5*mean);
		} catch (MathException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	// This method is used to calculate the boarding time of the passengers who came at facility during  
	// boarding of the first ones ("second wave"). Because of too long dwell time with full buses, the amount 
	// of passengers on-board is here not taken in account
	private double getStopTimeParallelWithoutOccupancyEffect(double leaving, double entering, MobsimVehicle vehicle) {
		double beta1 = vehicle.getVehicle().getType().getAccessTime();
		double beta2 = vehicle.getVehicle().getType().getEgressTime();
		double mean = Math.max(beta1*entering, beta2*leaving);	
		if(mean==0)
			return 0;
		double std = (2.2+0.12*mean);
		try {
			double r = MatsimRandom.getRandom().nextDouble();
			return Math.max(new NormalDistributionImpl(mean,std).inverseCumulativeProbability(r), 0.5*mean);
		} catch (MathException e) {
			e.printStackTrace();
		}
		return 0;
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
								this.passengersLeavingTimeFraction += vehicle.getVehicle().getType().getEgressTime();
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
									this.passengersEnteringTimeFraction += vehicle.getVehicle().getType().getAccessTime();
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



}
