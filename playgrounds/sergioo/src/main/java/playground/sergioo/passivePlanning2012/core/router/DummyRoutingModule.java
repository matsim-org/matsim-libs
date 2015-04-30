package playground.sergioo.passivePlanning2012.core.router;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;

public class DummyRoutingModule implements RoutingModule {
	
	public DummyRoutingModule() {
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Activity closestActivity = null;
		EmptyTime emptyTime = null;
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for(int i=0; i<planElements.size()-2; i++) {
			PlanElement planElement = planElements.get(i);
			if(planElement instanceof Activity && (closestActivity==null || Math.abs(closestActivity.getEndTime()-departureTime)>Math.abs(((Activity)planElement).getEndTime()-departureTime)) && ((Leg)planElements.get(i+1)) instanceof EmptyTime) {
				closestActivity = (Activity)planElement;
				emptyTime = ((EmptyTime)planElements.get(i+1));
			}
		}
		return Arrays.asList(emptyTime);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public String toString() {
		return "[DummyRoutingModule: mode=empty]";
	}
	
}
