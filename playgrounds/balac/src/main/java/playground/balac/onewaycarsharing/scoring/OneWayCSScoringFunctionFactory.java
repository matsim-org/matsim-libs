package playground.balac.onewaycarsharing.scoring;

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

import playground.balac.onewaycarsharing.config.OneWayCSConfigGroup;

public class OneWayCSScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory
{
  private final Config config;
  private final Network network;
  private final OneWayCSConfigGroup ftConfigGroup;
  
  public OneWayCSScoringFunctionFactory(Config config, OneWayCSConfigGroup ftConfigGroup, Network network)
  {
    super(config.planCalcScore(), network);
    this.network = network;
    this.config = config;
    this.ftConfigGroup = ftConfigGroup;
  }   

  public ScoringFunction createNewScoringFunction(Plan plan)
  {
	  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
    
    scoringFunctionAccumulator.addScoringFunction(
      new OneWayLegScoringFunction((PlanImpl)plan, 
      new CharyparNagelScoringParameters(config.planCalcScore()), 
      this.config, 
      this.ftConfigGroup, network));
    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
    scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
    //scoringFunctionAccumulator.addScoringFunction(new CarsharingScoringFunction());
    return scoringFunctionAccumulator;
  }
}
