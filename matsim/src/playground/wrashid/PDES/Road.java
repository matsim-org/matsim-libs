package playground.wrashid.PDES;

import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.plans.Person;

public class Road extends SimUnit {

	private Link link;
	private LinkedList gap;
	private LinkedList car;
	private LinkedList interestedInEnteringRoad;
	private double timeOfLastEnteringVehicle=Double.MIN_VALUE;
	// CONTINUE here.

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;
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
		interestedInEnteringRoad.add(vehicle);
		if (gap.size()>0){
			
		} else {
			
		}
	}

}
