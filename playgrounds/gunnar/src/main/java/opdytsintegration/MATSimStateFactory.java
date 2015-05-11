package opdytsintegration;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <X>
 *            the simulator state
 */
public interface MATSimStateFactory<X extends SimulatorState<X>, U extends DecisionVariable> {

	/**
	 * Creates a new object representation of the current MATSim simulation
	 * state.
	 * 
	 * @param population
	 *            the simulated MATSim population
	 * @param stateVector
	 *            a vector representation of the state to be created
	 * @param decisionVariable
	 *            the decision variable that has led to the state to be created
	 * @return the current MATSim simulation state
	 */
	public X newState(Population population, Vector stateVector,
			U decisionVariable);

}
