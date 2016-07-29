package opdytsintegration.roadinvestment;

import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

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
