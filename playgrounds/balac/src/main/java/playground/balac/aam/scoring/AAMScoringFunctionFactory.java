package playground.balac.aam.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.DesiresAndOpenTimesActivityScoring;



public class AAMScoringFunctionFactory implements ScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson parameters;

	public AAMScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    this.network = network;
	    this.config = config;
	    this.scenario = scenario;
		parameters = new SubpopulationCharyparNagelScoringParameters( scenario );

	  }   
	@Override
	  public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionSum = new SumScoringFunction();

		  scoringFunctionSum.addScoringFunction(
	      new AAMLegScoringFunction((PlanImpl)person.getSelectedPlan(),
				  new CharyparNagelScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build(),
	      this.config, 
	      network));
		  scoringFunctionSum.addScoringFunction(
				  new DesiresAndOpenTimesActivityScoring(
						  person.getSelectedPlan(),
						  parameters.getScoringParameters( person ),
						  scenario));
	    //scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));

		  scoringFunctionSum.addScoringFunction(
				  new CharyparNagelMoneyScoring(
						  parameters.getScoringParameters( person ) ));
		  scoringFunctionSum.addScoringFunction(
				  new CharyparNagelAgentStuckScoring(
						  parameters.getScoringParameters( person ) ));
	    return scoringFunctionSum;
	  }
}
