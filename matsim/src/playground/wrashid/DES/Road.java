package playground.wrashid.DES;

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
	private double inverseInFlowCapacity = 0;
	private double inverseOutFlowCapacity = 0;
	
	// also we keep track of the number of cars on the road, there is a problem:
	// if we schedule a car, that it may enter the road at time x, then must
	// keept track of the number of those cars, so that we can decide in enterRequest
	// how much space we have
	private int noOfCarsPromisedToEnterRoad=0;
	
	
	// how many cars can be parked on the street

	private long maxNumberOfCarsOnRoad = 0;

	// the time it takes for a gap to get to the back of the road
	private double gapTravelTime = 0;

	private LinkedList<Vehicle> carsOnTheRoad = new LinkedList<Vehicle>();
	private LinkedList<Double> earliestDepartureTimeOfCar = new LinkedList<Double>();

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;
		
		maxNumberOfCarsOnRoad = Math.round(link.getLength()
				* link.getLanesAsInt(SimulationParameters.linkCapacityPeriod)*SimulationParameters.storageCapacityFactor
				/ SimulationParameters.carSize);
		
		
		// this is an assumption made her: a road must at least have the space capacity to park one car
		// so that the backwar propagation of gaps can work
		if (maxNumberOfCarsOnRoad==0){
			maxNumberOfCarsOnRoad=1;
		}
		
		
		// System.out.println(maxNumberOfCars);

		double maxInverseInFlowCapacity = 3600/ (SimulationParameters.minimumInFlowCapacity *SimulationParameters.flowCapacityFactor);
		
		inverseOutFlowCapacity = 1 / (link
				.getFlowCapacity(SimulationParameters.linkCapacityPeriod)*SimulationParameters.flowCapacityFactor);
		
		
		if (inverseOutFlowCapacity>maxInverseInFlowCapacity){
			inverseInFlowCapacity=maxInverseInFlowCapacity;
		} else {
			inverseInFlowCapacity=inverseOutFlowCapacity;
		}
		
		
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
			double nextAvailableTimeForEnteringStreet = Math.max(
					timeOfLastEnteringVehicle + inverseInFlowCapacity,
					Scheduler.simTime + gapTravelTime);
			timeOfLastEnteringVehicle=nextAvailableTimeForEnteringStreet;
			noOfCarsPromisedToEnterRoad++;
			
			assert nextAvailableTimeForEnteringStreet > 0 : "ERROR: A car just left the street, so there should be at least a gap available (in future) to use the street!";
			
			// if the car is in ending leg mode, then it should give back its space on the street
			if (vehicle.isEndingLegMode()) {
				if (nextAvailableTimeForEnteringStreet > 0){
					giveBackPromisedSpaceToRoad();	
					sendMessage(new EndLegMessage(scheduler, vehicle), this
							.getUnitNo(), nextAvailableTimeForEnteringStreet);
				}
			} else {
				//System.out.println("###: " + nextAvailableTimeForEnteringStreet);
				if (nextAvailableTimeForEnteringStreet > 0){
				sendMessage(new EnterRoadMessage(scheduler, nextVehicle),
							this.getUnitNo(),
							nextAvailableTimeForEnteringStreet);
				}
			}
		}

		// tell the car behind the fist car (which is the first car now), when
		// it reaches the end of the read
		if (carsOnTheRoad.size() > 0) {
			Vehicle nextVehicle = carsOnTheRoad.getFirst();
			sendMessage(new EndRoadMessage(scheduler, nextVehicle), this
					.getUnitNo(), Math
					.max(earliestDepartureTimeOfCar.getFirst(),
							timeOfLastLeavingVehicle + inverseOutFlowCapacity));
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
		
		noOfCarsPromisedToEnterRoad--;
		carsOnTheRoad.add(vehicle);
		//System.out.println("carsOnTheRoad:" + carsOnTheRoad.size());
		//System.out.println("maxNumberOfCarsOnRoad:" + maxNumberOfCarsOnRoad);
		
		assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are more cars on the road, than its capacity!";
		earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

		// if we are in the front of the queue, then we can just drive with free
		// speed
		// to the front and have to have at least inverseFlowCapacity
		// time-distance to the
		// previous car
		if (carsOnTheRoad.size() == 1) {
			nextAvailableTimeForLeavingStreet = Math.max(
					nextAvailableTimeForLeavingStreet, timeOfLastLeavingVehicle
							+ inverseOutFlowCapacity);
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
		//System.out.println("cars on the road="+carsOnTheRoad.size());
		shrinkGapQueue();
		double nextAvailableTimeForEnteringStreet = Double.MIN_VALUE;

		//assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are more cars on the road, than its capacity!";
		assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size()+noOfCarsPromisedToEnterRoad : "You promised too many cars, that they can enter the street!";
		
		/*
		// enter this case, if the road is full (or planed to be full through promises)
		if (maxNumberOfCarsOnRoad == carsOnTheRoad.size()+noOfCarsPromisedToEnterRoad) {
			// the road is full, check if there are any gaps available
			if (gap.size() > 0) {
				nextAvailableTimeForEnteringStreet = Math.max(
						gap.removeFirst(), timeOfLastEnteringVehicle
								+ inverseInFlowCapacity);
				System.out.println("gap used");
				noOfCarsPromisedToEnterRoad++;
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
			// the current car should be able to enter the road immediately
			nextAvailableTimeForEnteringStreet = Math.max(
					timeOfLastEnteringVehicle + inverseInFlowCapacity,
					scheduler.simTime);
			// if another request arrives, it must conform to inverseFlowCapacity
			timeOfLastEnteringVehicle=nextAvailableTimeForEnteringStreet;
			noOfCarsPromisedToEnterRoad++;
			return nextAvailableTimeForEnteringStreet;
		}
		*/
		
		
		if (carsOnTheRoad.size()+noOfCarsPromisedToEnterRoad<maxNumberOfCarsOnRoad){
			// there is some space on the road (e.g. gaps)
			noOfCarsPromisedToEnterRoad++;
			//if (gap.size() > 0) {
			//	nextAvailableTimeForEnteringStreet = Math.max(
			//			gap.removeFirst(), timeOfLastEnteringVehicle
			//					+ inverseInFlowCapacity);
			//} else {
				// - the gaps have expired, so there must be some place in the street
				//   immediately
				// - the max of simTime is taken, because in the beginning the timeOfLastEnteringVehicle
				//   is Double.MIN_VALUE
				nextAvailableTimeForEnteringStreet = Math.max(
						timeOfLastEnteringVehicle + inverseInFlowCapacity,
						scheduler.simTime);
			//}
			timeOfLastEnteringVehicle=nextAvailableTimeForEnteringStreet;
			return nextAvailableTimeForEnteringStreet;
		} else {
			// at the moment, the road is full and no gap is available
			// => put this car into the interestedInEnteringRoad LinkedList
			// When cars leave the road, a gap slot will eventually be alloted to this car
			System.out.println("street full");
			interestedInEnteringRoad.add(vehicle);
			return -1.0;
		}
		
		
		
		
		
		
		

	}

	// remove all gaps, which are in the past
	private void shrinkGapQueue() {
		while (gap.size() > 0 && (Double) gap.get(0) < Scheduler.simTime) {
			gap.remove(0);
		}
	}
	
	public void giveBackPromisedSpaceToRoad(){
		noOfCarsPromisedToEnterRoad--;
	}

}
