package playground.wrashid.PDES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class EndRoadMessage extends SelfhandleMessage {
	private Vehicle vehicle;
	private Scheduler scheduler; // remove later from here...

	@Override
	public void selfhandleMessage() {
		// Find out, when this vehicle can enter the next road
		
		
		// leave previous road
		Road previousRoad=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		previousRoad.leaveRoad(vehicle);
		
		
	
		// enter next road
		vehicle.setLinkIndex(vehicle.getLinkIndex()+1);
		Link nextLink=vehicle.getCurrentLeg().getRoute().getLinkRoute()[vehicle.getLinkIndex()];
		
		Road nextRoad=Road.allRoads.get(nextLink.getId().toString());
		double nextAvailableTimeForEnteringStreet=nextRoad.enterRequest(vehicle);
		
		vehicle.setCurrentLink(nextLink);
		
		
		if (nextAvailableTimeForEnteringStreet>0){
			sendMessage(scheduler,new EnterRoadMessage(scheduler,vehicle), nextRoad.getUnitNo(), nextAvailableTimeForEnteringStreet);
		}
	}

	public EndRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}
	
	@Override
	public void printMessageLogString() {
		// the end of the road has been reached (this does not mean, that we can leave the road now...
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=leaving " );
		
	}

}
