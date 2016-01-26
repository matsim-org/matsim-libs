package floetteroed.opdyts.example.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.roadpricing.CalcPaidToll;

import com.google.inject.Inject;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * Returns the negative sum of the scores of the selected plans of all agents,
 * excluding toll.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingObjectiveFunction implements ObjectiveFunction {

	@Inject
	private CalcPaidToll calcPaidToll;

	private final double tollEffectivity;

	// private final Coord tollZoneCenter = CoordUtils
	// .createCoord(674000, 6581000);
	// private final double radius = 6000;

	public RoadpricingObjectiveFunction(final double tollEffectivity) {
		this.tollEffectivity = tollEffectivity;
	}

	private boolean isNearTollZone(final Plan plan) {
		return true;
		// for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
		// final Activity act = (Activity) plan.getPlanElements().get(i);
		// if (CoordUtils.calcDistance(act.getCoord(), this.tollZoneCenter) <
		// this.radius) {
		// return true;
		// }
		// }
		// return false;
	}

	@Override
	public double value(final SimulatorState state) {
		final RoadpricingState roadpricingState = (RoadpricingState) state;
		double result = -this.tollEffectivity * this.calcPaidToll.getAllAgentsToll();
		for (Id<Person> personId : roadpricingState.getPersonIdView()) {
			final Plan selectedPlan = roadpricingState
					.getSelectedPlan(personId);
			if (isNearTollZone(selectedPlan)) {
				result -= selectedPlan.getScore();
			}
		}
		result /= roadpricingState.getPersonIdView().size();
		return result;

	}
}
