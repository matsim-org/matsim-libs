package playground.wrashid.bsc.vbmh.vm_parking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * Adds the Park_Scoring function to the default scoring function
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class Park_Scoring_Factory extends CharyparNagelScoringFunctionFactory {

	public Park_Scoring_Factory(PlanCalcScoreConfigGroup config, Network network) {
		super(config, network);
		// TODO Auto-generated constructor stub
	}
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		SumScoringFunction scoringFunctionSum = (SumScoringFunction) super.createNewScoringFunction(plan);
		scoringFunctionSum.addScoringFunction(new Park_Scoring(plan));
		//ScoringFunctionAccumulator scoringFunctionAccumulator = (ScoringFunctionAccumulator)super.createNewScoringFunction(plan);
		//scoringFunctionAccumulator.addScoringFunction(new Park_Scoring());
		return scoringFunctionSum;
	}


}
