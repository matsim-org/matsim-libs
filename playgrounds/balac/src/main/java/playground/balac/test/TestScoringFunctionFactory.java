package playground.balac.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.DesiresAndOpenTimesActivityScoring;

public class TestScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	

	
	private final Config config;
	private final Network network;
	private final Scenario scenario;
	
	public TestScoringFunctionFactory(Config config, Network network,
			Scenario sc) {
		
		super(config.planCalcScore(), config.scenario(), network);
	    this.network = network;
	    this.config = config;
	    this.scenario = sc;
		
	}
	@Override
	 public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();

		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create(), network));
		  scoringFunctionAccumulator.addScoringFunction(new DesiresAndOpenTimesActivityScoring(person.getSelectedPlan(), CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create(), scenario));
	    //scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));

		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
	    return scoringFunctionAccumulator;
	  }

}
