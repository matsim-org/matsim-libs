package opdytsintegration.roadinvestment;

import java.util.Random;

import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadInvestmentStateFactory implements
		MATSimStateFactory<RoadInvestmentState, RoadInvestmentDecisionVariable> {

	public RoadInvestmentStateFactory() {
	}

	@Override
	public RoadInvestmentState newState(final Population population,
			final Vector stateVector,
			final RoadInvestmentDecisionVariable decisionVariable) {
		return new RoadInvestmentState(population, stateVector,
				decisionVariable != null ? decisionVariable.betaPay() : null,
				decisionVariable != null ? decisionVariable.betaAlloc() : null);
	}

}
