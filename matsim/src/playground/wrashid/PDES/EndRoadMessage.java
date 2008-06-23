package playground.wrashid.PDES;

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
		
		
	
		// enter next road link or start next leg
		if (vehicle.getCurrentLeg().getRoute().getLinkRoute().length==vehicle.getLinkIndex()+1){
			// the leg is completed, try to enter the last link but do not enter it 
			// (just wait, until you have clearance for enter and then leave the road)
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
			// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()+1)).getLink());
			
			System.out.println(vehicle.getCurrentLink().getId().toString());
			
			
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
			// start next leg
			// assumption: actions and legs are alternating in plans file
			vehicle.setLegIndex(vehicle.getLegIndex()+2);
			vehicle.setLinkIndex(-1);

			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
														// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			vehicle.setCurrentLeg((Leg) actsLegs.get(vehicle.getLegIndex()));
			// the leg the agent performs
			double departureTime = vehicle.getCurrentLeg().getDepTime(); // the time the agent
															// departs at this
															// activity

			
			// this is the link, where the first activity took place
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()-1)).getLink());

			sendMessage(scheduler,new StartingLegMessage(scheduler, vehicle), vehicle.unitNo, departureTime);
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
