package floetteroed.opdyts.filebased;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FileBasedSimulatorState implements SimulatorState {

	// -------------------- CONSTANTS --------------------

	private final Vector stateVector;

	private final double objectiveFunctionValue;

	// -------------------- CONSTRUCTION --------------------

	public FileBasedSimulatorState(final double objectiveFunctionValue,
			final Vector stateVector) {
		this.stateVector = stateVector;
		this.objectiveFunctionValue = objectiveFunctionValue;
	}

	// -------------------- GETTERS --------------------

	public double getObjectiveFunctionValue() {
		return this.objectiveFunctionValue;
	}

	// --------------- IMPLEMENTATION OF SimulatorState ---------------

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.stateVector;
	}

	@Override
	public void implementInSimulation() {
		/*
		 * Do nothing: Decision variables are already uniquely coupled to
		 * simulation trajectories.
		 */
	}
}
