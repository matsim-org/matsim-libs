package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class StartingLegMessage extends SelfhandleMessage {
	private Vehicle vehicle;

	// TODO: remove this scheduler from here
	// to somewhere else...
	Scheduler scheduler;

	public StartingLegMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}

	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=start leg" );
		//TODO: There is a difference in the link I get here and the one, which is used in DEQSim, because the one is using start link and the other is using end link
		// use the following normally: leg.getRoute().getLinkRoute()[linkIndex].getId().toString()
	}

	@Override
	public void selfhandleMessage() {
		
		// attempt to enter street.
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		double nextAvailableTimeForEnteringStreet=road.enterRequest(vehicle);
		if (nextAvailableTimeForEnteringStreet>0){
			sendMessage(scheduler,new EnterRoadMessage(scheduler,vehicle), road.getUnitNo(), nextAvailableTimeForEnteringStreet);
		}
	}
	
	
}
