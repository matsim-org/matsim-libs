package playground.thibautd.agentsmating.logitbasedmating.framework;

import playground.thibautd.agentsmating.logitbasedmating.basic.LogitModel;

/**
 * For use in test: a logit model with the same utility for all alternatives
 */
class DumbModel extends LogitModel {
	private final DecisionMakerFactory dmFactory = new DumbDecisionMakerFactory();
	private final ChoiceSetFactory csFactory = new DumbChoiceSetFactory();

	/**
	 * @return a {@link DumbDecisionMakerFactory}
	 */
	@Override
	public DecisionMakerFactory getDecisionMakerFactory() {
		return dmFactory;
	}

	/**
	 * @return a {@link DumbChoiceSetFactory}
	 */
	@Override
	public ChoiceSetFactory getChoiceSetFactory() {
		return csFactory;
	}

	@Override
	public double getSystematicUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) {
		return 1;
	}

	@Override
	public TripRequest changePerspective(
			final TripRequest tripToConsider,
			final TripRequest perspective) {
		throw new UnsupportedOperationException( "changePerspective is not implemented" );
	}
}
