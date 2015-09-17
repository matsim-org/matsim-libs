package playground.balac.twowaycarsharingredisigned.scoring;

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


public class TwoWayCSScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;

	public TwoWayCSScoringFunctionFactory(Config config, Network network)
	  {
		  super( null );
		  throw new RuntimeException( "this class does not do anything, so I did not refactor it. If you need to use it, talk with me. TD");
	    //super(config.planCalcScore(), config.scenario(), network);
	    //this.network = network;
	    //this.config = config;
	  }

//	  public ScoringFunction createNewScoringFunction(Plan plan) {
//
//		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
//
//		  scoringFunctionAccumulator.addScoringFunction(
//	      new TwoWayCSLegScoringFunction((PlanImpl)plan,
//				  CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create(),
//	      this.config,
//	      network));
//		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
//		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
//		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
//	    return scoringFunctionAccumulator;
//	  }
}
