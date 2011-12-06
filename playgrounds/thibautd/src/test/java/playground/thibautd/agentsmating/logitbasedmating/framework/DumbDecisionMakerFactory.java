package playground.thibautd.agentsmating.logitbasedmating.framework;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.agentsmating.logitbasedmating.basic.DecisionMakerImpl;

/**
 * for use in tests: creates {@link DecisionMakerImpl} instances with no special attributes
 */
class DumbDecisionMakerFactory implements DecisionMakerFactory {

	@Override
	public DecisionMaker createDecisionMaker(final Person agent)
			throws UnelectableAgentException {
		return new DecisionMakerImpl( (PersonImpl) agent );
	}
}
