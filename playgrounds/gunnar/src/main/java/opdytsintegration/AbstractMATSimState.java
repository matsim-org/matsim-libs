package opdytsintegration;

import optdyts.SimulatorState;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public abstract class AbstractMATSimState implements SimulatorState {

	// -------------------- CONSTANTS --------------------

	private final MATSimPopulationState populationState;

	private final Vector vectorRepresentation;

	// -------------------- CONSTRUCTION --------------------

	public AbstractMATSimState(final Population population,
			final Vector vectorRepresentation) {
		this.populationState = new MATSimPopulationState(population);
		this.vectorRepresentation = vectorRepresentation.copy();
	}

	// --------------- IMPLEMENTATION OF SimulatorState ---------------

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.vectorRepresentation;
	}

	@Override
	public void implementInSimulation() {
		this.populationState.implementInSimulation();
	}
}
