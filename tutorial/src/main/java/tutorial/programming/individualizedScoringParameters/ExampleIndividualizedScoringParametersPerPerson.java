package tutorial.programming.individualizedScoringParameters;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExampleIndividualizedScoringParametersPerPerson implements CharyparNagelScoringParametersForPerson {
	private final Scenario scenario;

	// For avoiding re-generating the parameters at each call, we store them in a map once created.
	private Map<Id<Person>,CharyparNagelScoringParameters> cache = new HashMap<>();

	@Inject
	public ExampleIndividualizedScoringParametersPerPerson( final Scenario scenario ) {
		this.scenario = scenario;
	}

	@Override
	public CharyparNagelScoringParameters getScoringParameters(Person person) {
		if ( cache.containsKey( person.getId() ) ) return cache.get( person.getId() );

		final CharyparNagelScoringParameters.Builder builder = new CharyparNagelScoringParameters.Builder(scenario, person.getId());

		// tune. Here hard-coded for lisibility, but should be computed/read from person attributes.
		builder.getActivityParameters( "h" ).setTypicalDuration_s( 8 * 3600 );
		builder.getModeParameters( "car" ).setMarginalUtilityOfTraveling_s( -6 );

		final CharyparNagelScoringParameters parameters = builder.build();
		cache.put( person.getId() , parameters );
		return parameters;
	}
}
