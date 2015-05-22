package opdytsintegration.zurichtunnel;

import java.util.List;
import java.util.Random;

import opdytsintegration.MATSimUnevaluatedState;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TunnelState extends MATSimUnevaluatedState<TunnelState> {

	// -------------------- CONSTANTS --------------------

	private Vector vectorRepresentation;

	// private Double betaPay;

	// private Double betaAlloc;

	private Double avgScore;

	// -------------------- CONSTRUCTION --------------------

	TunnelState(final Population population, final Vector vectorRepresentation,
	// final Double betaPay,
	// final Double betaAlloc,
			final Random rnd) {

		super(population, rnd);

		this.vectorRepresentation = vectorRepresentation.copy();
		// this.betaPay = betaPay;
		// this.betaAlloc = betaAlloc;

		double totalScore = 0.0;
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() != null) {
				totalScore += person.getSelectedPlan().getScore();
			}
		}
		this.avgScore = totalScore / population.getPersons().size();
	}

	private TunnelState(final double avgScore, final Vector vectorRepresentation) {
		super(null, null);
		this.avgScore = avgScore;
		this.vectorRepresentation = vectorRepresentation.copy();
	}

	// -------------------- GETTERS --------------------

	public double getAvgScore() {
		return this.avgScore;
	}

	// >>>>> TODO taken out person references >>>>>
	// double getAvgScore() {
	// double totalScore = 0.0;
	// for (Person person : this.person2planList.keySet()) {
	// totalScore += this.getSelectedPlan(person).getScore();
	// }
	// return totalScore / this.person2planList.size();
	// }
	// <<<<< TODO taken out person references <<<<<

	// public double getBetaPay() {
	// return this.betaPay;
	// }

	// public double getBetaAlloc() {
	// return this.betaAlloc;
	// }

	// IMPLEMENTATION OF SimulatorState / OVERRIDING OF UnevaluatedMATSimState

	@Override
	public TunnelState deepCopy() {
		if (this.population != null) {
			return new TunnelState(this.population, this.vectorRepresentation,
					this.rnd);
		} else {
			return new TunnelState(this.avgScore, this.vectorRepresentation);
		}
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return this.vectorRepresentation;
	}

	// @Override
	// @Deprecated
	// public void blendInOtherState(final TestZurichState otherState,
	// final double otherWeight) {
	//
	// super.blendInOtherState(otherState, otherWeight);
	//
	// final double myWeight = 1.0 - otherWeight;
	// this.vectorRepresentation.mult(myWeight);
	// this.vectorRepresentation.add(otherState.vectorRepresentation,
	// otherWeight);
	//
	// this.betaPay = myWeight * this.betaPay + otherWeight
	// * otherState.betaPay;
	// this.betaAlloc = myWeight * this.betaAlloc + otherWeight
	// * otherState.betaAlloc;
	//
	// }

	@Override
	public void takeOverConvexCombination(final List<TunnelState> states,
			final List<Double> weights) {

		// >>>>> TODO taken out person references >>>>>
		// TODO !!! This means that there is no convex population combination
		// !!!
		// super.takeOverConvexCombination(states, weights);
		// <<<<< TODO taken out person references <<<<<

		this.vectorRepresentation.clear();
		// this.betaPay = 0.0;
		// this.betaAlloc = 0.0;
		this.avgScore = 0.0;

		for (int i = 0; i < states.size(); i++) {
			final TunnelState state = states.get(i);
			final double weight = weights.get(i);
			this.vectorRepresentation.add(
					state.getReferenceToVectorRepresentation(), weight);
			// this.betaPay += state.getBetaPay() * weight;
			// this.betaAlloc += state.getBetaAlloc() * weight;
			this.avgScore += state.getAvgScore() * weight;
		}
	}

	@Override
	public void implementInSimulation() {
		super.implementInSimulation();
	}

	@Override
	public void releaseDeepMemory() {
		super.releaseDeepMemory();
	}

}
