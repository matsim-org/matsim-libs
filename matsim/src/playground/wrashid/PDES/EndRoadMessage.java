package playground.wrashid.PDES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class EndRoadMessage extends SelfhandleMessage {

	private Leg leg;
	private Vehicle vehicle;
	private int legIndex;
	private int linkIndex;
	private Link currentLink;
	private Scheduler scheduler; // remove later from here...

	@Override
	public void selfhandleMessage() {
		// Find out, when this vehicle can enter the next road
		Link nextLink=leg.getRoute().getLinkRoute()[linkIndex+1];
		
		Road road=Road.allRoads.get(nextLink.getId().toString());
		double nextAvailableTimeForEnteringStreet=road.enterRequest(vehicle);
		
		// leave previous road
		// TODO: program THIS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!#############
		
		// enter next road
		
		if (nextAvailableTimeForEnteringStreet>0){
			sendMessage(scheduler,new EnterRoadMessage(scheduler,leg,vehicle,legIndex,linkIndex+1,nextLink), road.getUnitNo(), nextAvailableTimeForEnteringStreet);
		}
	}

	public EndRoadMessage(Scheduler scheduler,Leg leg,Vehicle vehicle, int legIndex, int linkIndex, Link currentLink) {
		super();
		this.leg = leg;
		this.vehicle = vehicle;
		this.legIndex = legIndex;
		this.linkIndex=linkIndex;
		this.currentLink=currentLink;
		this.scheduler=scheduler;
	}
	
	@Override
	public void printMessageLogString() {
		// the end of the road has been reached (this does not mean, that we can leave the road now...
		//System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + currentLink.getId().toString() + "; Description=enter " );
		
	}

}
