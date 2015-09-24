package playground.balac.avignon;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityWOFacilitiesScoringFunction;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
/**
 * @author balacm
 */
public class AvignonScoringFunctionFactory implements ScoringFunctionFactory {

	private final Controler controler;
	private DestinationChoiceBestResponseContext lcContext;
	private Config config;
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);

	private final CharyparNagelScoringParametersForPerson parametersForPerson;

	public AvignonScoringFunctionFactory(Config config, Controler controler, DestinationChoiceBestResponseContext lcContext) {
		this.controler = controler;
		this.lcContext = lcContext;
		this.config = config;
		this.parametersForPerson = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
		log.info("creating DCScoringFunctionFactory");
	}
		
	private boolean usingConfigParamsForScoring = true ;
	public void setUsingConfigParamsForScoring( boolean val ) {
		usingConfigParamsForScoring = val ;
	}

//	@Override
//	public ScoringFunction createNewScoringFunction(Person person) {		
//ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator(); // TODO: replace this now by SumScore 
//		
//		CharyparNagelActivityScoring scoringFunction ;
//		if ( usingConfigParamsForScoring ) {
//			scoringFunction = new DCActivityWOFacilitiesScoringFunction(
//					person.getSelectedPlan(), 
//					this.lcContext);
//		} else {
//			scoringFunction = new DCActivityScoringFunction(
//					person.getSelectedPlan(), 
//					this.lcContext.getFacilityPenalties(), 
//					lcContext);
//		}
//		scoringFunctionAccumulator.addScoringFunction(scoringFunction);		
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), controler.getNetwork()));
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
//		return scoringFunctionAccumulator;
//	}
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {		
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		
		SumScoringFunction.BasicScoring scoringFunction ;
		if ( usingConfigParamsForScoring ) {
			scoringFunction = new DCActivityWOFacilitiesScoringFunction( person, this.lcContext);
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring( this.lcContext.getParams() ) ) ;
		} else {
			scoringFunction = new DCActivityScoringFunction(
					person.getSelectedPlan(),
                    lcContext);
		}

		final CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring( params ));
		return scoringFunctionAccumulator;
	}

}
