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
	
	public void enterRequest(Vehicle vehicle){
		System.out.println("enter request");
		interestedInEnteringRoad.add(vehicle);
		assert(false);
		if (numberOfVehiclesOnTheRoad==numberOfVehiclesOnTheRoad){
			
		} else {
			
		}
	}

}
