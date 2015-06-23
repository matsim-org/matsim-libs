package playground.balac.freefloating.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.KtiActivtyWithoutPenaltiesScoring;


public class FreeFloatingScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	  
	private final Scenario scenario;
	public FreeFloatingScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    super(config.planCalcScore(), network);
	    this.scenario = scenario;
	    this.network = network;
	    this.config = config;
	  }   

	  public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();

		  scoringFunctionAccumulator.addScoringFunction(
	      new FreeFloatingLegScoringFunction((PlanImpl)person.getSelectedPlan(),
				  CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(),
	      this.config, 
	      network));
		  scoringFunctionAccumulator.addScoringFunction(new KtiActivtyWithoutPenaltiesScoring(person.getSelectedPlan(), CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(), null, ((ScenarioImpl) scenario).getActivityFacilities()));

		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
		  scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
	   return scoringFunctionAccumulator;
	  }
}