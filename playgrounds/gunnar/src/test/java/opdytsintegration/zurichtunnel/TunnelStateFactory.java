package opdytsintegration.zurichtunnel;

import java.util.Random;

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
class TunnelStateFactory implements
		MATSimStateFactory {

	TunnelStateFactory(final Random rnd) {
	}

	@Override
	public MATSimState newState(Population population, Vector stateVector, DecisionVariable decisionVariable) {
		return new TunnelState(population, stateVector);
	}
}
