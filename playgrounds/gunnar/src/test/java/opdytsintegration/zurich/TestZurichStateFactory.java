package opdytsintegration.zurich;

import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TestZurichStateFactory implements
		MATSimStateFactory<TestZurichState, TestZurichDecisionVariable> {

	// private final Random rnd;

	TestZurichStateFactory() { // final Random rnd) {
	// this.rnd = rnd;
	}

	@Override
	public TestZurichState newState(final Population population,
			final Vector stateVector,
			final TestZurichDecisionVariable decisionVariable) {
		return new TestZurichState(population, stateVector,
				decisionVariable != null ? decisionVariable.betaPay() : null,
				decisionVariable != null ? decisionVariable.betaAlloc() : null
		// , this.rnd
		);
	}

}
