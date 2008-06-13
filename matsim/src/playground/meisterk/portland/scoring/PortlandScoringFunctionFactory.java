package playground.meisterk.portland.scoring;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

public class PortlandScoringFunctionFactory implements ScoringFunctionFactory {

	public ScoringFunction getNewScoringFunction(Plan plan) {
		return new PortlandScoringFunction(plan);
	}

}
