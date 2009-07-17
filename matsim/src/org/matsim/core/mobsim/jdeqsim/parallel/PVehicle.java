package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;


// TODO: only start using this class in parallel jdeqsim if the following methods are really
// needed ...
public class PVehicle extends Vehicle {

	public PVehicle(Scheduler scheduler, PersonImpl ownerPerson) {
		super(scheduler, ownerPerson);
		
	}



	// returns null, if there is no such Link, else the link
	// needed for locking...
	public LinkImpl getNextLinkInLeg() {
		int nextLinkIndex = getLinkIndex() + 1;
		LinkImpl[] linkRoute = getCurrentLinkRoute();

		if (linkRoute == null) {
			return null;
		}

		// if last link in leg, get first link of next leg
		if (isCurrentLegFinished()) {

			PlanImpl plan = getOwnerPerson().getSelectedPlan();
			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
			return ((ActivityImpl) actsLegs.get(getLegIndex() + 1)).getLink();

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
	public LinkImpl getPreviousLinkInLeg() {
		int previousLinkIndex = getLinkIndex() - 1;
		LinkImpl[] linkRoute = getCurrentLinkRoute();

		if (linkRoute == null) {
			return null;
		}

		// if first link in leg, get the link of the last activity
		if (previousLinkIndex < 0) {

			PlanImpl plan = getOwnerPerson().getSelectedPlan();
			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
			return ((ActivityImpl) actsLegs.get(getLegIndex() - 1)).getLink();

		} else {
			// return normal next link in leg

			return linkRoute[previousLinkIndex];
		}
	}

}
