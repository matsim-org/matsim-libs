package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.agentsmating.logitbasedmating.basic.AlternativeImpl;
import playground.thibautd.agentsmating.logitbasedmating.basic.TripRequestImpl;

/**
 * for use in test: creates a default choice set, where alternatives have no
 * special attribute
 */
class DumbChoiceSetFactory implements ChoiceSetFactory {

	@Override
	public List<Alternative> createChoiceSet(
			final DecisionMaker decisionMaker,
			final Plan plan,
			final int indexOfLeg) {
		List<Alternative> alts = new ArrayList<Alternative>();

		alts.add( new AlternativeImpl( "car" , new HashMap<String, Object>() ) );
		alts.add( new AlternativeImpl( "pt" , new HashMap<String, Object>() ) );
		alts.add( new AlternativeImpl( "bike" , new HashMap<String, Object>() ) );
		alts.add( new AlternativeImpl( "walk" , new HashMap<String, Object>() ) );
		alts.add( new TripRequestImpl( 
					TripRequestImpl.DRIVER_MODE,
					new HashMap<String, Object>(),
					indexOfLeg,
					(Activity) plan.getPlanElements().get( indexOfLeg - 1 ),
					(Activity) plan.getPlanElements().get( indexOfLeg + 1 ),
					12 * 3600d,
					decisionMaker,
					alts) );

		return alts;
	}
}
