package opdytsintegration.zurichtunnel;

import java.util.Random;

import opdytsintegration.MATSimPopulationState;
import optdyts.SimulatorState;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TunnelState implements SimulatorState<TunnelState> {

	// -------------------- CONSTANTS --------------------

	private final MATSimPopulationState populationState;

	private final Vector vectorRepresentation;

	private final double avgScore;

	// -------------------- CONSTRUCTION --------------------

	TunnelState(final Population population, final Vector vectorRepresentation,
			final Random rnd) {

		this.populationState = new MATSimPopulationState(population, rnd);
		this.vectorRepresentation = vectorRepresentation.copy();

		// double totalScore = 0.0;
		// int totalPlans = 0;
		// for (Person person : population.getPersons().values()) {
		// for (Plan plan : person.getPlans()) {
		// totalScore += plan.getScore();
		// }
		// totalPlans += person.getPlans().size();
		// }
		// this.avgScore = totalScore / totalPlans;

		double totalScore = 0.0;
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() != null) {
				totalScore += person.getSelectedPlan().getScore();
			}
		}
		this.avgScore = totalScore / population.getPersons().size();
	}

//	private TunnelState(final TunnelState parent) {
//		if (parent.populationState != null) {
//			this.populationState = parent.populationState.copy();
//		} else {
//			this.populationState = null;
//		}
//		this.vectorRepresentation = parent.vectorRepresentation.copy();
//		this.avgScore = parent.avgScore;
//	}

	// -------------------- GETTERS --------------------

	double getAvgScore() {
		return this.avgScore;
	}

	// --------------- IMPLEMENTATION OF SimulatorState ---------------

//	@Override
//	public TunnelState deepCopy() {
//		return new TunnelState(this);
//	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.vectorRepresentation;
	}

//	@Override
//	public void takeOverConvexCombination(final List<TunnelState> states,
//			final List<Double> weights) {
//
//		// No convex population combination needed.
//		this.populationState = null;
//
//		// No convex vector representation is needed.
//		this.vectorRepresentation = null;
//
//		// this.vectorRepresentation.clear();
//		this.avgScore = 0.0;
//		for (int i = 0; i < states.size(); i++) {
//			final TunnelState state = states.get(i);
//			final double weight = weights.get(i);
//			// this.vectorRepresentation.add(
//			// state.getReferenceToVectorRepresentation(), weight);
//			this.avgScore += state.getAvgScore() * weight;
//		}
//	}

	@Override
	public void implementInSimulation() {
		this.populationState.implementInSimulation();
	}

//	@Override
//	public void releaseDeepMemory() {
//		this.populationState = null;
//		this.vectorRepresentation = null; // TODO this is new
//	}
}
