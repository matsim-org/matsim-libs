package floetteroed.opdyts.example;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VectorState implements SimulatorState {

	private final Vector x;
	
	private final Vector prevU;

	private final LinearSystemSimulator system;

	public VectorState(final Vector x, final Vector prevU, final LinearSystemSimulator system) {
		this.x = x.copy();
		this.prevU = prevU.copy();
		this.system = system;
	}

	Vector getX() {
		return this.x;
	}
	
	Vector getPrevU() {
		return this.prevU;
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
