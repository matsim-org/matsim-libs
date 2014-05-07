package playground.balac.onewaycarsharingredisgned.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class OneWayCarsharingRDScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	  
	public OneWayCarsharingRDScoringFunctionFactory(Config config, Network network)
	  {
	    super(config.planCalcScore(), network);
	    this.network = network;
	    this.config = config;
	  }   

	  public ScoringFunction createNewScoringFunction(Plan plan) {
		  
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
	    
		  scoringFunctionAccumulator.addScoringFunction(
	      new OneWayCarsharingRDLegScoringFunction((PlanImpl)plan, 
	      new CharyparNagelScoringParameters(config.planCalcScore()), 
	      this.config, 
	      network));
	    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    return scoringFunctionAccumulator;
	  }
}
