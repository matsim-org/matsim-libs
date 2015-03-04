package playground.balac.onewaycarsharingredisgned.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.DesiresAndOpenTimesActivityScoring;


public class OneWayCarsharingRDScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	private final Scenario scenario;
	  
	public OneWayCarsharingRDScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    super(config.planCalcScore(), network);
	    this.scenario = scenario;
	    this.network = network;
	    this.config = config;
	  }   

	@Override
	  public ScoringFunction createNewScoringFunction(Person person) {
		  
		  SumScoringFunction scoringFunctionSum = new SumScoringFunction();
		    
		  scoringFunctionSum.addScoringFunction(
	      new OneWayCarsharingRDLegScoringFunction((PlanImpl)person.getSelectedPlan(), 
	      new CharyparNagelScoringParameters(config.planCalcScore()), 
	      this.config, 
	      network));
		  scoringFunctionSum.addScoringFunction(new DesiresAndOpenTimesActivityScoring(person.getSelectedPlan(), new CharyparNagelScoringParameters(config.planCalcScore()), scenario));
		   
		  scoringFunctionSum.addScoringFunction(new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		  scoringFunctionSum.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    return scoringFunctionSum;
	  }
}
