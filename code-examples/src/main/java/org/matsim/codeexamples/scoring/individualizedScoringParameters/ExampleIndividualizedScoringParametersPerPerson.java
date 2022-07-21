package org.matsim.codeexamples.scoring.individualizedScoringParameters;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExampleIndividualizedScoringParametersPerPerson implements ScoringParametersForPerson {
	private final Scenario scenario;

	// For avoiding re-generating the parameters at each call, we store them in a map once created.
	private Map<Id<Person>,ScoringParameters> cache = new HashMap<>();

	@Inject
	public ExampleIndividualizedScoringParametersPerPerson( final Scenario scenario ) {
		this.scenario = scenario;
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {
		if ( cache.containsKey( person.getId() ) ) return cache.get( person.getId() );

		final ScoringParameters.Builder builder = new ScoringParameters.Builder(scenario, person);

		// tune the following. Here hard-coded for legibility, but should be computed/read from person attributes.

		ActivityUtilityParameters.Builder actParamsBuilder = new ActivityUtilityParameters.Builder( );
		actParamsBuilder.setType( "h" );
		actParamsBuilder.setTypicalDuration_s( 8. * 3600. );
		actParamsBuilder.setZeroUtilityComputation( new ActivityUtilityParameters.SameRelativeScore() ); // (yyyy should become default)
		builder.setActivityParameters( "h", actParamsBuilder.build() );

		ModeUtilityParameters.Builder modeParamsBuilder = new ModeUtilityParameters.Builder();
		modeParamsBuilder.setMarginalUtilityOfTraveling_s( -6./3600. );
		builder.setModeParameters( "car", modeParamsBuilder.build() );

		// the design of the above has changed with Tilmanns re-design of the income-dependent activity parameters; there does not seem to be
		// a formulation that works both for 13.x and 14.x.  kai, jun'21

		final ScoringParameters parameters = builder.build();
		cache.put( person.getId() , parameters );
		return parameters;
	}
}
