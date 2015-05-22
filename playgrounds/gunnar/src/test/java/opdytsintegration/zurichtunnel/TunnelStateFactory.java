package opdytsintegration.zurichtunnel;

import java.util.Random;

import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TunnelStateFactory implements
		MATSimStateFactory<TunnelState, TunnelConfiguration> {

	private final Random rnd;

	public TunnelStateFactory(final Random rnd) {
		this.rnd = rnd;
	}

	@Override
	public TunnelState newState(final Population population,
			final Vector stateVector, final TunnelConfiguration decisionVariable) {
		return new TunnelState(population, stateVector, this.rnd);
	}

}
