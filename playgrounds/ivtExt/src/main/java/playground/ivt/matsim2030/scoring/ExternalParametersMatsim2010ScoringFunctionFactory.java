package playground.ivt.matsim2030.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.socnetsim.jointtrips.scoring.BlackListedActivityScoringFunction;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.kticompatibility.KtiActivityScoring;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.scoring.LineChangeScoringFunction;

/**
 * @author thibautd
 */
public class ExternalParametersMatsim2010ScoringFunctionFactory implements ScoringFunctionFactory {
	private final Scenario scenario;
	private final CharyparNagelScoringParametersForPerson parametersForPerson;
	// TODO modularize
	private final StageActivityTypes typesNotToScore = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

	@Inject
	public ExternalParametersMatsim2010ScoringFunctionFactory(Scenario scenario, CharyparNagelScoringParametersForPerson parametersForPerson) {
		this.scenario = scenario;
		this.parametersForPerson = parametersForPerson;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		// get scenario elements at the lattest possible, to be sure all is initialized
		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();

		final DestinationChoiceBestResponseContext locationChoiceContext = (DestinationChoiceBestResponseContext)
			scenario.getScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME );

		final SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		final CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );

		// XXX THIS is the difference with default scoring function. Incorporate in the core and get rid of home-brewed
		// scoring function factories. Home-brewed parametersPerPerson should be enough
		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(
					typesNotToScore,
					new KtiActivityScoring(
						person.getSelectedPlan(),
						params,
						scenario.getActivityFacilities() )) );

		// standard modes
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelLegScoring(
						params,
						scenario.getNetwork(),
						scenario.getTransitSchedule() ) );

		scoringFunctionAccumulator.addScoringFunction(
				new LineChangeScoringFunction(
					config ) );

		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ));
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));

		if ( locationChoiceContext != null ) {
			scoringFunctionAccumulator.addScoringFunction(
					new DestinationEspilonScoring(
						person,
						locationChoiceContext ) );
		}

		return scoringFunctionAccumulator;
	}
}
