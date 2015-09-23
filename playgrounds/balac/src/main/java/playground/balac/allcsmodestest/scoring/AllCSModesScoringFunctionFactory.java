package playground.balac.allcsmodestest.scoring;

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

public class AllCSModesScoringFunctionFactory implements ScoringFunctionFactory {
	
	private final Config config;
	private final Network network;
	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson parametersForPerson;

	public AllCSModesScoringFunctionFactory(Config config, Network network, Scenario scenario)
	  {
	    this.network = network;
	    this.config = config;
	    this.scenario = scenario;
		this.parametersForPerson = new SubpopulationCharyparNagelScoringParameters( scenario );
	  }   
	@Override
	  public ScoringFunction createNewScoringFunction(Person person)
	  {
		  SumScoringFunction scoringFunctionSum = new SumScoringFunction();
	    //this is the main difference, since we need a special scoring for carsharing legs
		  scoringFunctionSum.addScoringFunction(
	      new CarsharingWithTaxiLegScoringFunction((PlanImpl)person.getSelectedPlan(),
				  parametersForPerson.getScoringParameters( person ),
	      this.config,
	      network));
		  //the remaining scoring functions can be changed and adapted to the needs of the user
		  scoringFunctionSum.addScoringFunction(
				  new DesiresAndOpenTimesActivityScoring(
						  person.getSelectedPlan(),
						  parametersForPerson.getScoringParameters( person ),
						  scenario));
		  scoringFunctionSum.addScoringFunction(
				  new CharyparNagelMoneyScoring(
						  parametersForPerson.getScoringParameters( person ) ) );
		  scoringFunctionSum.addScoringFunction(
				  new CharyparNagelAgentStuckScoring(
						  parametersForPerson.getScoringParameters( person ) ) );
	    return scoringFunctionSum;
	  }
}
