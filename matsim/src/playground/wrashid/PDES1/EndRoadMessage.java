package playground.wrashid.PDES1;

import java.util.ArrayList;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkLeave;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class EndRoadMessage extends EventMessage {
// TODO: This is not a normal Event message, perhaps I should redesign it and put it somewhere else
	// in the class hierarchy

	@Override
	public void selfhandleMessage() {
		// Find out, when this vehicle can enter the next road
	
		
		// leave previous road
		//Road previousRoad=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		//previousRoad.leaveRoad(vehicle);
		System.out.println("end road message");
		
		
		if (vehicle.getCurrentLinkRoute().length==vehicle.getLinkIndex()+1){
			// the leg is completed, try to enter the last link but do not enter it 
			// (just wait, until you have clearance for enter and then leave the road)
			
			
			Road previousRoad=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			
			vehicle.initiateEndingLegMode();
			
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
			// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()+1)).getLink());
			
			//System.out.println(vehicle.getCurrentLink().getId().toString());
			
			Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			//road.enterRequest(vehicle);	
			road.roadEntryHandler.registerEnterRequestMessage(previousRoad, vehicle, messageArrivalTime);
		} else if (vehicle.getCurrentLinkRoute().length>vehicle.getLinkIndex()+1){
			// if leg is not finished yet
			
			
			Road previousRoad=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			
			vehicle.setLinkIndex(vehicle.getLinkIndex()+1);
			Link nextLink=vehicle.getCurrentLinkRoute()[vehicle.getLinkIndex()];
			
			Road nextRoad=Road.allRoads.get(nextLink.getId().toString());
			vehicle.setCurrentLink(nextLink);
			//nextRoad.enterRequest(vehicle);
			nextRoad.roadEntryHandler.registerEnterRequestMessage(previousRoad, vehicle, messageArrivalTime);
		} else {
			
		}
	}

	public EndRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType="";
		logMessage=false;
	}
	
	public void logEvent() {
		// don't do anything
	}

	@Override
	public void recycleMessage() {
		MessageFactory.disposeEndRoadMessage(this);
	}

}
