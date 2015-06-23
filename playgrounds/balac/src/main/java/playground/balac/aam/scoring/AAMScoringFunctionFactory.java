package playground.balac.aam.scoring;

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



public class AAMScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	private final Scenario scenario;

	public AAMScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    super(config.planCalcScore(), network);
	    this.network = network;
	    this.config = config;
	    this.scenario = scenario;

	  }   
	@Override
	  public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionSum = new SumScoringFunction();

		  scoringFunctionSum.addScoringFunction(
	      new AAMLegScoringFunction((PlanImpl)person.getSelectedPlan(),
				  CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(),
	      this.config, 
	      network));
		  scoringFunctionSum.addScoringFunction(new DesiresAndOpenTimesActivityScoring(person.getSelectedPlan(), CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(),  scenario));
	    //scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));

		  scoringFunctionSum.addScoringFunction(new CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
		  scoringFunctionSum.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
	    return scoringFunctionSum;
	  }
}
