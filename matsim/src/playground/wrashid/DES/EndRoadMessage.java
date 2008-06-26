package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Act;
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
		
		
	
		
		if (vehicle.getCurrentLeg().getRoute().getLinkRoute().length==vehicle.getLinkIndex()+1){
			// the leg is completed, try to enter the last link but do not enter it 
			// (just wait, until you have clearance for enter and then leave the road)
			
			vehicle.initiateEndingLegMode();
			
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
			// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()+1)).getLink());
			
			//System.out.println(vehicle.getCurrentLink().getId().toString());
			
			Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			double nextAvailableTimeForEnteringStreet=road.enterRequest(vehicle);
			
			
			
			
			
			if (nextAvailableTimeForEnteringStreet>0){
				
				// attention: as we are not actually entering the road, we need to give back the promised space to the road
				// else a precondition of the enterRequest would not be correct any more (which involves the noOfCarsPromisedToEnterRoad variable)
				road.giveBackPromisedSpaceToRoad();
				sendMessage(scheduler,new EndLegMessage(scheduler,vehicle), road.getUnitNo(), nextAvailableTimeForEnteringStreet);
			}
			
		} else if (vehicle.getCurrentLeg().getRoute().getLinkRoute().length>vehicle.getLinkIndex()+1){
			// if leg is not finished yet
			vehicle.setLinkIndex(vehicle.getLinkIndex()+1);
			Link nextLink=vehicle.getCurrentLeg().getRoute().getLinkRoute()[vehicle.getLinkIndex()];
			
			Road nextRoad=Road.allRoads.get(nextLink.getId().toString());
			double nextAvailableTimeForEnteringStreet=nextRoad.enterRequest(vehicle);
			
			vehicle.setCurrentLink(nextLink);
			
			
			if (nextAvailableTimeForEnteringStreet>0){
				sendMessage(scheduler,new EnterRoadMessage(scheduler,vehicle), nextRoad.getUnitNo(), nextAvailableTimeForEnteringStreet);
			}
		} else {
			
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
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=leave link" );
	}

}
