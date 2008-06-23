package playground.wrashid.PDES;

import org.matsim.network.Link;
import org.matsim.plans.Leg;

public class EnterRoadMessage extends SelfhandleMessage {

	private Vehicle vehicle;
	private Scheduler scheduler; // remove later from here...

	@Override
	public void selfhandleMessage() {
		// TODO Auto-generated method stub
		//Continue here: ask road to really enter the road
		// => Road will then let us enter the road and tell us, when we can leave the road.
		
		
		
		
		// enter the road and find out the time for leaving the street
		
		
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		double nextAvailableTimeForLeavingStreet=road.enterRoad(vehicle);
		if (nextAvailableTimeForLeavingStreet>0){
			sendMessage(scheduler,new EndRoadMessage(scheduler,vehicle), road.getUnitNo(), nextAvailableTimeForLeavingStreet);
		}
		
		
		
		
	}

	public EnterRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}
	
	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=enter " );
		
	}

}
