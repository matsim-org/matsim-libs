package playground.wrashid.PDES;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.plans.Person;

public class Road extends SimUnit {

	public static HashMap<String,Road> allRoads;
	private Link link;
	private LinkedList gap=new LinkedList();
	private LinkedList car=new LinkedList();
	private LinkedList interestedInEnteringRoad=new LinkedList();
	private double timeOfLastEnteringVehicle=Double.MIN_VALUE;
	private int numberOfVehiclesOnTheRoad=0;
	// the inverseFlowCapacity is simple the inverse
	// of the capacity meaning, the minimal time between two cars entering/leaving the road
	private double inverseFlowCapacity=0;
	// how many cars can be parked on the street
	// size of one car is assumed 7.5m
	private long maxNumberOfCars=0;
	// CONTINUE here.

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;
		Math.round(33.0);
		maxNumberOfCars=Math.round(link.getLength()*link.getLanesAsInt(SimulationParameters.linkCapacityPeriod)/7.5);
		//System.out.println(maxNumberOfCars);
		inverseFlowCapacity=1/link.getCapacity(SimulationParameters.linkCapacityPeriod);
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
	
	// gives back the time, when the car can enter the road
	// it returns -1, if there is no space in the street and the car will be handled later
	// => TODO: remove the return value. Scheduling the car etc. should be done by the vehicle
	public double enterRequest(Vehicle vehicle){
		shrinkGapQueue();
		double nextAvailableTimeForEnteringStreet=Double.MIN_VALUE;
		System.out.println("enter request");
		interestedInEnteringRoad.add(vehicle);
		assert(maxNumberOfCars>=numberOfVehiclesOnTheRoad);
		if (numberOfVehiclesOnTheRoad==maxNumberOfCars){
			// the road is full, check if there are any gaps available
			if (gap.size()>0){
				nextAvailableTimeForEnteringStreet=Math.max((Double)gap.get(0),timeOfLastEnteringVehicle+inverseFlowCapacity);
				gap.remove(0);
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
			// simple check, that the time distance between two cars should be at least
			// inverseFlowCapacity
			// of course, if the last car entered the road more than inverseFlowCapacity time ago, then
			// the current car should be able to enter the road immediatly
			nextAvailableTimeForEnteringStreet=Math.max(timeOfLastEnteringVehicle+inverseFlowCapacity,scheduler.simTime);
		}
		// the following code should never be executed!!!!!
		assert(false);
		return -1;
	}
	
	// remove all gaps, which are in the past
	private void shrinkGapQueue(){
		while (gap.size()>0){
			if ((Double)gap.get(0)<Scheduler.simTime){
				gap.remove(0);
			}
		}
	}

}
