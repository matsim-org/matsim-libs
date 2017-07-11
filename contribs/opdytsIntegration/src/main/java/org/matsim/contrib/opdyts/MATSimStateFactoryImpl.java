package org.matsim.contrib.opdyts;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <U>
 *            decision variable type
 */
public class MATSimStateFactoryImpl<U extends DecisionVariable> implements MATSimStateFactory<U> {

	protected Controler controler;

	public MATSimStateFactoryImpl() {
	}

	@Override
	public void registerControler(final Controler controler) {
		this.controler = controler;
	}

	@Override
	public MATSimState newState(final Population population, final Vector stateVector, final U decisionVariable) {
		return new MATSimState(population, stateVector);
	}

}
