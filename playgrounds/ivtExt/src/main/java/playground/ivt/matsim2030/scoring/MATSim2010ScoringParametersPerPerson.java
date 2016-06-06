package playground.ivt.matsim2030.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

import java.util.*;

/**
 * @author thibautd
 */
@Singleton
public class MATSim2010ScoringParametersPerPerson implements CharyparNagelScoringParametersForPerson {
	private final Scenario scenario;
	private final Map<Id<Person>, CharyparNagelScoringParameters> cache = new HashMap<>();

	private final StageActivityTypes typesNotToScore = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE );

	@Inject
	public MATSim2010ScoringParametersPerPerson(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public CharyparNagelScoringParameters getScoringParameters(Person person) {
		if ( cache.containsKey( person.getId() ) ) {
			return cache.get( person.getId() );
		}

		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();
		final ScenarioConfigGroup scenarioConfig = scenario.getConfig().scenario();
		final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();

		final CharyparNagelScoringParameters.Builder builder =
				new CharyparNagelScoringParameters.Builder(
						config,
						config.getScoringParameters(null),
						scenarioConfig);

		final Set<String> handledTypes = new HashSet<>();
		for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , typesNotToScore ) ) {
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
									new PlanCalcScoreConfigGroup.ActivityParams( act.getType() ) );

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

		// KTI like consideration of influence of travel card
		// (except that is was not expressed as a ratio)
		final KtiLikeScoringConfigGroup ktiConfig = (KtiLikeScoringConfigGroup)
				scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME );
		final Collection<String> travelCards = PersonUtils.getTravelcards(person);
		if ( travelCards != null && !travelCards.isEmpty() ) {
			builder.getModeParameters( TransportMode.pt ).setMarginalUtilityOfDistance_m(
					config.getModes().get( TransportMode.pt ).getMarginalUtilityOfDistance() * ktiConfig.getTravelCardRatio() );
		}

		final CharyparNagelScoringParameters params =
				builder.build();
		cache.put( person.getId() , params );
		return params;
	}
}
