package playground.wrashid.PDES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class StartingLegMessage extends SelfhandleMessage {
	private Leg leg=null;
	private Vehicle vehicle;
	// at which position in plan.getActsLegs(), the current Leg is located
	// this is needed for finding out, which leg to take next
	private int legIndex;
	// A route is made up of several links. So we need the index of the link here.
	private int linkIndex;
	// the link, at which the leg starts
	private Link startingLink;
	// TODO: remove this scheduler from here
	// to somewhere else...
	Scheduler scheduler;

	public StartingLegMessage(Scheduler scheduler,Leg leg,Vehicle vehicle, int legIndex, int linkIndex, Link startingLink) {
		super();
		this.leg = leg;
		this.vehicle = vehicle;
		this.legIndex = legIndex;
		this.linkIndex=linkIndex;
		this.startingLink=startingLink;
		this.scheduler=scheduler;
	}

	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + startingLink.getId().toString() + "; Description=starting " );
		//TODO: There is a difference in the link I get here and the one, which is used in DEQSim, because the one is using start link and the other is using end link
		// use the following normally: leg.getRoute().getLinkRoute()[linkIndex].getId().toString()
	}

	public int getLegIndex() {
		return legIndex;
	}

	public int getLinkIndex() {
		return linkIndex;
	}

	@Override
	public void selfhandleMessage() {
		
		// attempt to enter street.
		Road road=Road.allRoads.get(startingLink.getId().toString());
		double nextAvailableTimeForEnteringStreet=road.enterRequest(vehicle);
		if (nextAvailableTimeForEnteringStreet>0){
			sendMessage(scheduler,new EnterRoadMessage(scheduler,leg,vehicle,legIndex,linkIndex,startingLink), road.getUnitNo(), nextAvailableTimeForEnteringStreet);
		}
	}
	
	
}
