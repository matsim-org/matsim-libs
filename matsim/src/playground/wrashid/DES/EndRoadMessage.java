package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.population.Act;
import org.matsim.population.Plan;

public class EndRoadMessage extends EventMessage {

	@Override
	public void handleMessage() {
		if (vehicle.isCurrentLegFinished()) {
			// the leg is completed, try to enter the last link but do not enter
			// it
			// (just wait, until you have clearance for enter and then leave the
			// road)

			vehicle.initiateEndingLegMode();

			Plan plan = vehicle.getOwnerPerson().getSelectedPlan();
			
			vehicle.moveToFirstLinkInNextLeg();
			Road road = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());
			road.enterRequest(vehicle);
		} else if (!vehicle.isCurrentLegFinished()) {
			// if leg is not finished yet
			vehicle.moveToNextLinkInLeg();

			Road nextRoad = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());
			nextRoad.enterRequest(vehicle);
		}
	}

	public EndRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
	}

	public void processEvent() {
		// don't need to output any event
	}

}
