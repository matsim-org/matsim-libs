package opdytsintegration.example.networkparameters;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NetworkParametersStateFactory implements MATSimStateFactory<NetworkParameters> {

	@Override
	public MATSimState newState(Population population, Vector stateVector, NetworkParameters decisionVariable) {
		return new NetworkParametersState(population, stateVector);
	}
}
