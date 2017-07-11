package org.matsim.contrib.opdyts;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;

/**
 * A factory for MATSim simulation states.
 * 
 * @author Gunnar Flötteröd
 *
 * @see MATSimState
 * @see DecisionVariable
 */
public interface MATSimStateFactory<U extends DecisionVariable> {

	/**
	 * Because the controler is created after this factory.
	 * 
	 * TODO What could this be good for? Is it used at all?
	 */
	public void registerControler(final Controler controler);

	/**
	 * Creates a new object representation of the current MATSim simulation
	 * state.
	 * 
	 * IMPORTANT: Do not take over a controler reference into the state object
	 * and attempt to compute state properties (such as objective function
	 * values) on the fly. Instead, compute all relevant state attributes
	 * explicitly when creating the state object.
	 * 
	 * @see MATSimState
	 * 
	 * @param population
	 *            the current MATSim population
	 * @param stateVector
	 *            a vector representation of the state to be created
	 * @param decisionVariable
	 *            the decision variable that has led to the state to be created
	 * @return the current MATSim simulation state
	 */
	public MATSimState newState(Population population, Vector stateVector, U decisionVariable);

}
