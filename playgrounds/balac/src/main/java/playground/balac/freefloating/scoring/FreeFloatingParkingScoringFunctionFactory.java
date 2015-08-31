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
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.balac.allcsmodestest.scoring.KtiActivtyWithoutPenaltiesScoring;

public class FreeFloatingParkingScoringFunctionFactory implements ScoringFunctionFactory{
	
	private ParkingScoreManager parkingScoreManager;
	private Controler controler;

	public FreeFloatingParkingScoringFunctionFactory(
			Controler controler, ParkingScoreManager parkingScoreManager) {

		super();
		this.controler = controler;
		this.parkingScoreManager = parkingScoreManager;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
		
		
		scoringFunctionSum.addScoringFunction(
			      new FreeFloatingLegScoringFunction((PlanImpl)person.getSelectedPlan(), 
			    		  CharyparNagelScoringParameters.getBuilder(this.controler.getConfig().planCalcScore()).create(), 
			      this.controler.getConfig(), 
			      this.controler.getScenario().getNetwork()));
		scoringFunctionSum.addScoringFunction(new KtiActivtyWithoutPenaltiesScoring(person.getSelectedPlan(),  CharyparNagelScoringParameters.getBuilder(this.controler.getConfig().planCalcScore()).create(), null, ((ScenarioImpl) this.controler.getScenario()).getActivityFacilities()));
				   
		scoringFunctionSum.addScoringFunction(new CharyparNagelMoneyScoring( CharyparNagelScoringParameters.getBuilder(this.controler.getConfig().planCalcScore()).create()));
		scoringFunctionSum.addScoringFunction(new CharyparNagelAgentStuckScoring( CharyparNagelScoringParameters.getBuilder(this.controler.getConfig().planCalcScore()).create()));
		scoringFunctionSum.addScoringFunction(new ParkingScoringFunction(person
				.getSelectedPlan(),parkingScoreManager));
		return scoringFunctionSum;
	}

}
