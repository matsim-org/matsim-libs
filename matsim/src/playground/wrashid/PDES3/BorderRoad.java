package playground.wrashid.PDES3;

import org.matsim.network.Link;

import playground.wrashid.DES.Road;
import playground.wrashid.DES.Scheduler;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.DES.Vehicle;

public class BorderRoad extends Road {

	private int startingRoadZoneId = 0;
	private int endingRoadZoneId = 0;

	public BorderRoad(Scheduler scheduler, Link link, int startingRoadZoneId,
			int endingRoadZoneId) {
		super(scheduler, link);
		this.startingRoadZoneId = startingRoadZoneId;
		this.endingRoadZoneId = endingRoadZoneId;
	}

	public void enterRequest(Vehicle vehicle, double simTime) {
		synchronized (this) {
			super.enterRequest(vehicle, simTime);
		}
	}

	// see documentation in class Road
	public void enterRoad(Vehicle vehicle, double simTime) {
		synchronized (this) {
			double nextAvailableTimeForLeavingStreet = Double.MIN_VALUE;
			nextAvailableTimeForLeavingStreet = simTime
					+ link.getLength()
					/ link
							.getFreespeed(SimulationParameters.linkCapacityPeriod);

			noOfCarsPromisedToEnterRoad--;
			carsOnTheRoad.add(vehicle);

			earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

			if (carsOnTheRoad.size() == 1) {
				nextAvailableTimeForLeavingStreet = Math.max(
						nextAvailableTimeForLeavingStreet,
						timeOfLastLeavingVehicle + inverseOutFlowCapacity);
				// This line of code needs to be changed (schedule not a message
				// in the current thread, but in the road end thread)
				vehicle.scheduleEndRoadMessage(
						nextAvailableTimeForLeavingStreet, this);
			} else {

			}
		}
	}

	public void leaveRoad(Vehicle vehicle, double simTime) {
		synchronized (this) {
			super.leaveRoad(vehicle, simTime);
		}
	}

}
