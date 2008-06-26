package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class EndLegMessage extends SelfhandleMessage {
	private Vehicle vehicle;

	Scheduler scheduler;

	public EndLegMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}

	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=end leg" );
	}

	@Override
	public void selfhandleMessage() {
		
		// schedule next leg, if there are more legs, else end trip (TODO)
		
		// start next leg
		// assumption: actions and legs are alternating in plans file
		vehicle.setLegIndex(vehicle.getLegIndex()+2);
		vehicle.setLinkIndex(-1);
		
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
														// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
		if ((actsLegs.size()>vehicle.getLegIndex())){	
			vehicle.setCurrentLeg((Leg) actsLegs.get(vehicle.getLegIndex()));
			// the leg the agent performs
			double departureTime = vehicle.getCurrentLeg().getDepTime(); // the time the agent
															// departs at this
															// activity
			
			
			// if the departureTime for the leg is in the past, then set it to the current simulation time
			// this avoids that messages in the past are put into the scheduler
			if (departureTime<scheduler.getSimTime()){
				departureTime=scheduler.getSimTime();
			}
			
	
			
			// this is the link, where the first activity took place
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()-1)).getLink());
	
			sendMessage(scheduler,new StartingLegMessage(scheduler, vehicle), vehicle.unitNo, departureTime);
		}
		
	}
	
	
}
