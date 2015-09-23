package playground.balac.freefloating.scoring;

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
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.KtiActivtyWithoutPenaltiesScoring;


public class FreeFloatingScoringFunctionFactory implements ScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	private final CharyparNagelScoringParametersForPerson parametersForPerson;
	  
	private final Scenario scenario;
	public FreeFloatingScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    this.scenario = scenario;
	    this.network = network;
	    this.config = config;
		  this.parametersForPerson = new SubpopulationCharyparNagelScoringParameters( scenario );
	  }   

	  @Override
	public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();

		  final CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );
		  scoringFunctionAccumulator.addScoringFunction(
	      new FreeFloatingLegScoringFunction((PlanImpl)person.getSelectedPlan(),
				  params,
				  this.config,
				  network));
		  scoringFunctionAccumulator.addScoringFunction(
				  new KtiActivtyWithoutPenaltiesScoring(
						  person.getSelectedPlan(),
						  params,
						  null,
						  scenario.getActivityFacilities()));

		  scoringFunctionAccumulator.addScoringFunction(
				  new CharyparNagelMoneyScoring(
						  params ) );
		  scoringFunctionAccumulator.addScoringFunction(
				  new CharyparNagelAgentStuckScoring(
						  params ) );
	   return scoringFunctionAccumulator;
	  }
}