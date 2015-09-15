package playground.vbmh.vmParking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * Adds the ParkScoring function to the default scoring function
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkScoringFactory extends CharyparNagelScoringFunctionFactory {

	public ParkScoringFactory(PlanCalcScoreConfigGroup config, ScenarioConfigGroup scenarioConfig, Network network) {
		super(config, scenarioConfig, network);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = (SumScoringFunction) super.createNewScoringFunction(person);
		scoringFunctionSum.addScoringFunction(new ParkScoring(person.getSelectedPlan()));
		//ScoringFunctionAccumulator scoringFunctionAccumulator = (ScoringFunctionAccumulator)super.createNewScoringFunction(plan);
		//scoringFunctionAccumulator.addScoringFunction(new ParkScoring());
		return scoringFunctionSum;
	}


}
