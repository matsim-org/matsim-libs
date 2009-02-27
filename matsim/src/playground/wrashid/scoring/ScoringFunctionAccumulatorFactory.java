package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

public class ScoringFunctionAccumulatorFactory implements ScoringFunctionFactory {

	public ScoringFunction getNewScoringFunction(Plan plan) {
		// TODO Auto-generated method stub
		return new ScoringFunctionAccumulator(plan);
	}

}
