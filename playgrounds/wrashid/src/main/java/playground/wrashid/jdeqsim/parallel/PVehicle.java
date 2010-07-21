package playground.wrashid.jdeqsim.parallel;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.mobsim.jdeqsim.Vehicle;



public class PVehicle extends Vehicle {

	public PVehicle(Scheduler scheduler, Person ownerPerson) {
		super(scheduler, ownerPerson);

	}



	// returns null, if there is no such Link, else the link
	// needed for locking...
	public Id getNextLinkInLeg() {
		int nextLinkIndex = getLinkIndex() + 1;
		Id[] linkRoute = getCurrentLinkRoute();

		if (linkRoute == null) {
			return null;
		}

		// if last link in leg, get first link of next leg
		if (isCurrentLegFinished()) {

			Plan plan = getOwnerPerson().getSelectedPlan();
			List<? extends PlanElement> actsLegs = plan.getPlanElements();
			return ((Activity) actsLegs.get(getLegIndex() + 1)).getLinkId();

		} else {
			// return normal next link in leg

			if (nextLinkIndex < linkRoute.length) {
				return linkRoute[nextLinkIndex];
			} else {
				return null;
			}
		}
	}

	// returns null, if there is no such Link, else the link
	// needed for locking...
	public Id getPreviousLinkInLeg() {
		int previousLinkIndex = getLinkIndex() - 1;
		Id[] linkRoute = getCurrentLinkRoute();

		if (linkRoute == null) {
			return null;
		}

		// if first link in leg, get the link of the last activity
		if (previousLinkIndex < 0) {

			Plan plan = getOwnerPerson().getSelectedPlan();
			List<? extends PlanElement> actsLegs = plan.getPlanElements();
			return ((Activity) actsLegs.get(getLegIndex() - 1)).getLinkId();

		} else {
			// return normal next link in leg

			return linkRoute[previousLinkIndex];
		}
	}

}
