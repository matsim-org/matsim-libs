package opdytsintegration.roadinvestment;

import floetteroed.opdyts.DecisionVariable;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadInvestmentStateFactory implements
		MATSimStateFactory {

	public RoadInvestmentStateFactory() {
	}

	@Override
	public MATSimState newState(Population population, Vector stateVector, DecisionVariable decisionVariable) {
		return new RoadInvestmentState(population, stateVector,
				decisionVariable != null ? ((RoadInvestmentDecisionVariable) decisionVariable).betaPay() : null,
				decisionVariable != null ? ((RoadInvestmentDecisionVariable) decisionVariable).betaAlloc() : null);
	}

}
