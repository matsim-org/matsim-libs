package opdytsintegration;

import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <U>
 *            decision variable type
 */
public class MATSimStateFactoryImpl<U extends DecisionVariable> implements
		MATSimStateFactory<U> {

	public MATSimStateFactoryImpl() {
	}

	@Override
	public MATSimState newState(final Population population,
			final Vector stateVector, final U decisionVariable) {
		return new MATSimState(population, stateVector);
	}

}
