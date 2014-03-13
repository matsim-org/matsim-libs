package playground.wrashid.bsc.vbmh.vm_parking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
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


public class ParkScoringFactory extends CharyparNagelScoringFunctionFactory {

	public ParkScoringFactory(PlanCalcScoreConfigGroup config, Network network) {
		super(config, network);
		// TODO Auto-generated constructor stub
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
