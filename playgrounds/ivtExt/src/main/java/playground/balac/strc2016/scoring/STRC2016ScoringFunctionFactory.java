package playground.balac.strc2016.scoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.google.inject.Inject;

public class STRC2016ScoringFunctionFactory implements ScoringFunctionFactory{

	private Scenario scenario;
	private CharyparNagelScoringParametersForPerson parameters;
	private final Map<Id, CharyparNagelScoringParameters> individualParameters = new HashMap< >();

	@Inject
	public STRC2016ScoringFunctionFactory(
			Scenario scenario) {
		this.scenario = scenario;
		this.parameters = new SubpopulationCharyparNagelScoringParameters( scenario );
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		final SumScoringFunction scoringFunctionSum = new SumScoringFunction();
		ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
		final PlanCalcScoreConfigGroup config = scenario.getConfig().planCalcScore();

		CharyparNagelScoringParameters  params = createParams( person ,config , scenario.getConfig().scenario(), personAttributes );
		
		addScoringFunction(person.getId(), scoringFunctionSum, new CharyparNagelMoneyScoring(
				parameters.getScoringParameters( person ) ) );
		
		addScoringFunction(person.getId(), scoringFunctionSum, new CharyparNagelLegScoring( 
				params,this.scenario.getNetwork()
				 ));
		addScoringFunction(person.getId(), scoringFunctionSum, new KtiActivtyWithoutPenaltiesScoring(
				person.getSelectedPlan(),
				parameters.getScoringParameters( person ),
				null,
				this.scenario.getActivityFacilities()));
		
		addScoringFunction(person.getId(), scoringFunctionSum, new CharyparNagelMoneyScoring(
				parameters.getScoringParameters( person ) ) );	
		
		addScoringFunction(person.getId(), scoringFunctionSum, new CharyparNagelAgentStuckScoring(
				parameters.getScoringParameters( person ) ) );	
		
	
		
		return scoringFunctionSum;
	}
	
	private CharyparNagelScoringParameters createParams(
			final Person person,
			final PlanCalcScoreConfigGroup config,
			final ScenarioConfigGroup scenarioConfig,
			final ObjectAttributes personAttributes) {
		if ( individualParameters.containsKey( person.getId() ) ) {
			return individualParameters.get( person.getId() );
		}

		final CharyparNagelScoringParameters.Builder builder =
				new CharyparNagelScoringParameters.Builder(config, config.getScoringParameters(null), scenarioConfig);
		final Set<String> handledTypes = new HashSet<String>();
		for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , null ) ) {
			
			//get utility of performing if available from the person attributes
			
			final Double performingUtility =
					(Double) personAttributes
					.getAttribute(person.getId().toString(), "performing_" + act.getType());
			
			if (performingUtility != null) 
				builder.setMarginalUtilityOfPerforming_s(performingUtility);
			
			final Double travelingCarUtility = (-1.0) * 
					(Double) personAttributes
					.getAttribute(person.getId().toString(), "traveling_car" ) / 3600.0;
			
			builder.setModeParameters("car",
					new ModeUtilityParameters.Builder()
						.setMarginalUtilityOfTraveling_s( travelingCarUtility )
						.setConstant( config.getScoringParameters( null ).getOrCreateModeParams("car").getConstant() ) );
			
			
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
	
	
	
	private void addScoringFunction(
			final Id<Person> person,
			final SumScoringFunction function,
			final BasicScoring element ) {
		//tracker.addScoringFunction(person, element);
		function.addScoringFunction(element);
	}

}
