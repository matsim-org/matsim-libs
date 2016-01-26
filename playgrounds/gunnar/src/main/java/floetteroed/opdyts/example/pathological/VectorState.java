package floetteroed.opdyts.example.pathological;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VectorState implements SimulatorState {

	private final Vector x;

	private final LinearSystemSimulator system;

	public VectorState(final Vector x, final LinearSystemSimulator system) {
		this.x = x.copy();
		this.system = system;
	}

	Vector getX() {
		return this.x;
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.getX();
	}

	@Override
	public void implementInSimulation() {
		this.system.setState(this.x);
	}
}
