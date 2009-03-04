package org.matsim.scoring.charyparNagel;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionAccumulator;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;

public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;

	public CharyparNagelScoringFunctionFactory(final CharyparNagelScoringConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}

	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * 
	 * @param plan
	 * @return
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));

		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, params));

		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));

		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;
	}
}
