package sharedmobility;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.socnetsim.jointtrips.scoring.BlackListedActivityScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters.Builder;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.ivt.kticompatibility.KtiActivityScoring;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.scoring.DestinationEspilonScoring;
import playground.ivt.scoring.LineChangeScoringFunction;

/**
 * @author thibautd, hbecker
 */
public class SharedMobilityScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final StageActivityTypes blackList;

	// very expensive to initialize:only do once!
	private final Map<Id, CharyparNagelScoringParameters> individualParameters = new HashMap< >();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public SharedMobilityScoringFunctionFactory(
			final Scenario scenario,
			final StageActivityTypes typesNotToScore ) {
		this.scenario = scenario;
		this.blackList = typesNotToScore;
	}


	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		// get scenario elements at the lattest possible, to be sure all is initialized
		final KtiLikeScoringConfigGroup ktiConfig = (KtiLikeScoringConfigGroup)
			scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME );
		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();

		final DestinationChoiceBestResponseContext locationChoiceContext = (DestinationChoiceBestResponseContext)
			scenario.getScenarioElement( DestinationChoiceBestResponseContext.ELEMENT_NAME );
		final ObjectAttributes personAttributes =
				scenario.getPopulation().getPersonAttributes();

		final SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		//final ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		final CharyparNagelScoringParameters params = createParams( person , config , scenario.getConfig().scenario(), personAttributes );

		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(
					blackList,
					new KtiActivityScoring(
						person.getSelectedPlan(),
						params,
						((MutableScenario) scenario).getActivityFacilities() )) );

		// standard modes
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.car,
					LegScoringParameters.createForCar(
						params ),
					scenario.getNetwork()));
		
		
		// car-sharing
				scoringFunctionAccumulator.addScoringFunction(
						new ElementalCharyparNagelLegScoringFunction(
							TransportMode.car,
							LegScoringParameters.createForCar(
								params ),
							scenario.getNetwork()));	
		
		
		// KTI like consideration of influence of travel card
		// (except that is was not expressed as a ratio)
		final Collection<String> travelCards = PersonUtils.getTravelcards(person);
		final double utilityOfDistancePt =
			travelCards == null || travelCards.isEmpty() ?
				params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m :
				params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * ktiConfig.getTravelCardRatio();
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.pt,
					new LegScoringParameters(
						params.modeParams.get(TransportMode.pt).constant,
						params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s,
						utilityOfDistancePt),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.walk,
					LegScoringParameters.createForWalk(
						params ),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.bike,
					LegScoringParameters.createForBike(
							params),
					scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new ElementalCharyparNagelLegScoringFunction(
					TransportMode.transit_walk,
					LegScoringParameters.createForWalk(
							params),
					scenario.getNetwork()));
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

	private CharyparNagelScoringParameters createParams(
			final Person person,
			final PlanCalcScoreConfigGroup config,
			final ScenarioConfigGroup scenarioConfig,
			final ObjectAttributes personAttributes) {
		if ( individualParameters.containsKey( person.getId() ) ) {
			return individualParameters.get( person.getId() );
		}

		final Builder builder =
				new Builder(config, config.getScoringParameters(null), scenarioConfig);
		final Set<String> handledTypes = new HashSet<String>();
		for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , blackList ) ) {
			// XXX works only if no variation of type of activities between plans
			if ( !handledTypes.add( act.getType() ) ) continue; // parameters already gotten

			final String id = person.getId().toString();

			// I am not so pleased with this, as wrong parameters may silently be
			// used (for instance if individual preferences are ill-specified).
			// This should become nicer once we have a better format for specifying
			// utility parameters in the config.
			final ActivityUtilityParameters.Builder typeBuilder =
					new ActivityUtilityParameters.Builder(
							config.getActivityParams( act.getType() ) != null ?
									config.getActivityParams( act.getType() ) :
									new ActivityParams( act.getType() ) );

			final Double earliestEndTime =
					(Double) personAttributes.getAttribute(
						id,
						"earliestEndTime_"+act.getType() );
			if ( earliestEndTime != null ) {
				typeBuilder.setScoreAtAll(true);
				typeBuilder.setEarliestEndTime( earliestEndTime );
			}

			final Double latestStartTime =
					(Double) personAttributes.getAttribute(
						id,
						"latestStartTime_"+act.getType() );
			if ( latestStartTime != null ) {
				typeBuilder.setScoreAtAll(true);
				typeBuilder.setLatestStartTime(latestStartTime);
			}

			final Double minimalDuration =
					(Double) personAttributes.getAttribute(
						id,
						"minimalDuration_"+act.getType() );
			if ( minimalDuration != null ) {
				typeBuilder.setScoreAtAll( true );
				typeBuilder.setMinimalDuration(minimalDuration);
			}

			final Double typicalDuration =
					(Double) personAttributes.getAttribute(
						id,
						"typicalDuration_"+act.getType() );
			if ( typicalDuration != null ) {
				typeBuilder.setScoreAtAll( true );
				typeBuilder.setTypicalDuration_s(typicalDuration);
			}

			builder.setActivityParameters(
					act.getType(),
					typeBuilder );
		}

		final CharyparNagelScoringParameters params =
				builder.build();
		individualParameters.put( person.getId() , params );
		return params;
	}
}

