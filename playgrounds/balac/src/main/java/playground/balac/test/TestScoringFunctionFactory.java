package playground.balac.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.DesiresAndOpenTimesActivityScoring;

public class TestScoringFunctionFactory implements ScoringFunctionFactory {
	

	
	private final Config config;
	private final Network network;
	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson parameters;
	
	public TestScoringFunctionFactory(Config config, Network network,
			Scenario sc) {
		
	    this.network = network;
	    this.config = config;
	    this.scenario = sc;

		this.parameters = new SubpopulationCharyparNagelScoringParameters( sc );
	}
	@Override
	 public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();

		  final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

		  scoringFunctionAccumulator.addScoringFunction(
				  new CharyparNagelLegScoring(
						  params,
						  network));
		  scoringFunctionAccumulator.addScoringFunction(
				  new DesiresAndOpenTimesActivityScoring(
						  person.getSelectedPlan(),
						  params,
						  scenario));
	    //scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));

		  scoringFunctionAccumulator.addScoringFunction(
				  new CharyparNagelMoneyScoring(
						  params ) );
		  scoringFunctionAccumulator.addScoringFunction(
				  new CharyparNagelAgentStuckScoring(
						  params ) );
	    return scoringFunctionAccumulator;
	  }

}
