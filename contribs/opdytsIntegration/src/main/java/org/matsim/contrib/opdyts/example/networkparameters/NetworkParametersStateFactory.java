package org.matsim.contrib.opdyts.example.networkparameters;

import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;
import org.matsim.contrib.opdyts.MATSimStateFactory;
import org.matsim.core.controler.Controler;

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

	@Override
	public void registerControler(Controler controler) {
	}
}
