package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;

/**
 * 
 * @author wrashid
 *
 */
public class EVScoringFunctionFactory extends MATSim2010ScoringFunctionFactory {

	private StageActivityTypes typesNotToScore;
	private Scenario scenario;

	public EVScoringFunctionFactory(Scenario scenario,
			StageActivityTypes typesNotToScore) {
		super(scenario, typesNotToScore);
		this.scenario = scenario;
		this.typesNotToScore = typesNotToScore;
	}
	
	 @Override
	  public ScoringFunction createNewScoringFunction(Person person) {
		 SumScoringFunction scoringFunctionAccumulator = (SumScoringFunction)super.createNewScoringFunction(person);
		 scoringFunctionAccumulator.addScoringFunction(new EVCVScoringFunction(person));
	    return scoringFunctionAccumulator;
	  }

}
