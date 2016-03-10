package floetteroed.opdyts.filebased;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FileBasedObjectiveFunction implements ObjectiveFunction {

	// -------------------- CONSTRUCTION --------------------

	public FileBasedObjectiveFunction() {
	}

	// --------------- IMPLEMENTATION OF ObjectiveFunction ---------------

	@Override
	public double value(final SimulatorState state) {
		return ((FileBasedSimulatorState) state).getObjectiveFunctionValue();
	}

}
