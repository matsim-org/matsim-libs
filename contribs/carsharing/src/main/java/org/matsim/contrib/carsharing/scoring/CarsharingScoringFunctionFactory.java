package org.matsim.contrib.carsharing.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CarsharingScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	
	private final Config config;
	private final Network network;

	public CarsharingScoringFunctionFactory(Config config, Network network)
	  {
	    super(config.planCalcScore(), network);
	    this.network = network;
	    this.config = config;

	  }   
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
	    //this is the main difference, since we need a special scoring for carsharing legs
		scoringFunctionSum.addScoringFunction(
	    new CarsharingLegScoringFunction(new CharyparNagelScoringParameters(config.planCalcScore()), 
	    								 this.config, 
	    								 this.network));
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelLegScoring( 
			    new CharyparNagelScoringParameters(config.planCalcScore()), this.network) 
			    );
		//the remaining scoring functions can be changed and adapted to the needs of the user
		scoringFunctionSum.addScoringFunction(new CharyparNagelActivityScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		scoringFunctionSum.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
	    return scoringFunctionSum;
	  }
}
