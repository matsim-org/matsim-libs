package playground.balac.freefloating.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunction;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.KtiActivtyWithoutPenaltiesScoring;

public class FreeFloatingParkingScoringFunctionFactory implements ScoringFunctionFactory{
	
	private ParkingScoreManager parkingScoreManager;
	private Controler controler;
	private ScoringParametersForPerson parameters;

	public FreeFloatingParkingScoringFunctionFactory(
			Controler controler, ParkingScoreManager parkingScoreManager) {
		this.controler = controler;
		this.parkingScoreManager = parkingScoreManager;
		this.parameters = new SubpopulationScoringParameters( controler.getScenario() );
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
		
		
		scoringFunctionSum.addScoringFunction(
			      new FreeFloatingLegScoringFunction( (PlanImpl) person.getSelectedPlan(),
						  parameters.getScoringParameters( person ),
						  this.controler.getConfig(),
						  this.controler.getScenario().getNetwork()));
		scoringFunctionSum.addScoringFunction(
				new KtiActivtyWithoutPenaltiesScoring(
						person.getSelectedPlan(),
						parameters.getScoringParameters( person ),
						null,
						this.controler.getScenario().getActivityFacilities()));
				   
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelMoneyScoring(
						parameters.getScoringParameters( person ) ) );
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelAgentStuckScoring(
						parameters.getScoringParameters( person ) ) );
		scoringFunctionSum.addScoringFunction(new ParkingScoringFunction(person
				.getSelectedPlan(),parkingScoreManager));
		return scoringFunctionSum;
	}

}
