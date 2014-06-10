package playground.balac.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
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
		
		super(config.planCalcScore(), network);
	    this.network = network;
	    this.config = config;
	    this.scenario = sc;
		
	}
	
	 public ScoringFunction createNewScoringFunction(Plan plan)
	  {
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
	    
		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), network));
	    scoringFunctionAccumulator.addScoringFunction(new DesiresAndOpenTimesActivityScoring(plan, new CharyparNagelScoringParameters(config.planCalcScore()), ((ScenarioImpl) scenario).getActivityFacilities()));
	    //scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		   
	    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    return scoringFunctionAccumulator;
	  }

}
