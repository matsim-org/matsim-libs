package playground.vbmh.vmParking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * Adds the ParkScoring function to the default scoring function
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkScoringFactory implements ScoringFunctionFactory {
	private final CharyparNagelScoringFunctionFactory delegate;

	public ParkScoringFactory( final Scenario scenario ) {
		this.delegate = new CharyparNagelScoringFunctionFactory( scenario );
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = (SumScoringFunction) delegate.createNewScoringFunction(person);
		scoringFunctionSum.addScoringFunction(new ParkScoring(person.getSelectedPlan()));
		//ScoringFunctionAccumulator scoringFunctionAccumulator = (ScoringFunctionAccumulator)super.createNewScoringFunction(plan);
		//scoringFunctionAccumulator.addScoringFunction(new ParkScoring());
		return scoringFunctionSum;
	}


}
