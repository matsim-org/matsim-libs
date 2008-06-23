package playground.wrashid.PDES;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.plans.Person;

public class Road extends SimUnit {

	public static HashMap<String, Road> allRoads;
	private Link link;
	private LinkedList<Double> gap = new LinkedList<Double>();
	private LinkedList<Vehicle> interestedInEnteringRoad = new LinkedList<Vehicle>();
	private double timeOfLastEnteringVehicle = Double.MIN_VALUE;
	private double timeOfLastLeavingVehicle = Double.MIN_VALUE;

	// the inverseFlowCapacity is simple the inverse
	// of the capacity meaning, the minimal time between two cars
	// entering/leaving the road
	private double inverseFlowCapacity = 0;
	// how many cars can be parked on the street
	// size of one car is assumed 7.5m
	private long maxNumberOfCarsOnRoad = 0;

	// the time it takes for a gap to get to the back of the road
	private double gapTravelTime = 0;

	private LinkedList<Vehicle> carsOnTheRoad = new LinkedList<Vehicle>();
	private LinkedList<Double> earliestDepartureTimeOfCar = new LinkedList<Double>();

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;
		Math.round(33.0);
		maxNumberOfCarsOnRoad = Math.round(link.getLength()
				* link.getLanesAsInt(SimulationParameters.linkCapacityPeriod)
				/ 7.5);
		// System.out.println(maxNumberOfCars);
		inverseFlowCapacity = 1 / link
				.getCapacity(SimulationParameters.linkCapacityPeriod);

		gapTravelTime = link.getLength() / SimulationParameters.gapTravelSpeed;

	}

	@Override
	public void finalize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleMessage(Message m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	public void leaveRoad(Vehicle vehicle) {
		carsOnTheRoad.removeFirst();
		earliestDepartureTimeOfCar.removeFirst();
		timeOfLastLeavingVehicle = Scheduler.simTime;

		// produce a gap on the road
		gap.add(Scheduler.simTime + gapTravelTime);

		// the next car waiting for entering the road should now be alloted a
		// time for entering the road
		if (interestedInEnteringRoad.size() > 0) {
			Vehicle nextVehicle = interestedInEnteringRoad.removeFirst();
			double nextAvailableTimeForEnteringStreet = this
					.enterRequest(nextVehicle);

			if (vehicle.isEndingLegMode()) {
				if (nextAvailableTimeForEnteringStreet > 0) {
					sendMessage(new EndLegMessage(scheduler, vehicle), this
							.getUnitNo(), nextAvailableTimeForEnteringStreet);
				}
			} else {
				if (nextAvailableTimeForEnteringStreet > 0) {
					sendMessage(new EnterRoadMessage(scheduler, nextVehicle),
							this.getUnitNo(),
							nextAvailableTimeForEnteringStreet);
				}
			}
		}

		// tell the car behind the fist car (which is the first car now), when
		// it can leave the street
		if (carsOnTheRoad.size() > 0) {
			Vehicle nextVehicle = carsOnTheRoad.getFirst();
			sendMessage(new EndRoadMessage(scheduler, nextVehicle), this
					.getUnitNo(), Math
					.max(earliestDepartureTimeOfCar.getFirst(),
							timeOfLastLeavingVehicle + inverseFlowCapacity));
		}

	}

	// returns the time, when the car reaches the end of the road
	// TODO: instead of returning the scheduling time, just schedule messages
	// here...
	public double enterRoad(Vehicle vehicle) {
		double nextAvailableTimeForLeavingStreet = Double.MIN_VALUE;
		nextAvailableTimeForLeavingStreet = Scheduler.simTime
				+ link.getLength()
				/ link.getFreespeed(SimulationParameters.linkCapacityPeriod);
		carsOnTheRoad.add(vehicle);
		earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

		// if we are in the front of the queue, then we can just drive with free
		// speed
		// to the front and have to have at least inverseFlowCapacity
		// time-distance to the
		// previous car
		if (carsOnTheRoad.size() == 1) {
			nextAvailableTimeForLeavingStreet = Math.max(
					nextAvailableTimeForLeavingStreet, timeOfLastLeavingVehicle
							+ inverseFlowCapacity);
			return nextAvailableTimeForLeavingStreet;
		} else {
			// this car is not the front car in the street queue
			// when the cars infront of the current car leave the street and
			// this car becomes the
			// front car, it will be waken up...
			return -1.0;
		}

	}

	// gives back the time, when the car can enter the road
	// it returns -1, if there is no space in the street and the car will be
	// handled later
	// => TODO: remove the return value. Scheduling the car etc. should be done
	// by the vehicle
	public double enterRequest(Vehicle vehicle) {
		shrinkGapQueue();
		double nextAvailableTimeForEnteringStreet = Double.MIN_VALUE;

		assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are more cars on the road, than its capacity!";
		if (maxNumberOfCarsOnRoad == carsOnTheRoad.size()) {
			// the road is full, check if there are any gaps available
			if (gap.size() > 0) {
				nextAvailableTimeForEnteringStreet = Math.max(
						gap.removeFirst(), timeOfLastEnteringVehicle
								+ inverseFlowCapacity);

				return nextAvailableTimeForEnteringStreet;
			} else {
				// at the moment, the road is full and no gap is available
				// => put this car into the interestedInEnteringRoad LinkedList
				// so, when a car leaves, we assign that slot to this car
				interestedInEnteringRoad.add(vehicle);
				return -1.0;
			}
		} else {
			// there is space on the road for more cars
			// simple check, that the time distance between two cars should be
			// at least
			// inverseFlowCapacity
			// of course, if the last car entered the road more than
			// inverseFlowCapacity time ago, then
			// the current car should be able to enter the road immediatly
			nextAvailableTimeForEnteringStreet = Math.max(
					timeOfLastEnteringVehicle + inverseFlowCapacity,
					scheduler.simTime);
			return nextAvailableTimeForEnteringStreet;
		}

	}

	// remove all gaps, which are in the past
	private void shrinkGapQueue() {
		while (gap.size() > 0) {
			if ((Double) gap.get(0) < Scheduler.simTime) {
				gap.remove(0);
			}
		}
	}

}
