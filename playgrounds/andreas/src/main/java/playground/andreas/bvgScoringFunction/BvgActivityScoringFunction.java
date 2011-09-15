package playground.andreas.bvgScoringFunction;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.pt.PtConstants;

public class BvgActivityScoringFunction extends ActivityScoringFunction {

	private CharyparNagelScoringParameters params;

	public BvgActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params) {
		super(params);
		this.params = params;
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime, Activity act) {
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			return (departureTime - arrivalTime) * this.params.marginalUtilityOfWaiting_s;
		} else {
			return super.calcActScore(arrivalTime, departureTime, act);
		}
	}

}
