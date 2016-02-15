package floetteroed.opdyts.ntimestworoutes;

import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesSimulator<U extends DecisionVariable> implements Simulator<U> {

	// -------------------- CONSTANTS (CONFIGURABLE) --------------------

	private final List<TwoRoutesReplanner> replanners;

	// private final double demand;

	private final double capacity;

	// private final Vector externalities;

	// -------------------- MEMBERS --------------------

	private Vector tolls = null;

	// -------------------- CONSTRUCTION --------------------

	NTimesTwoRoutesSimulator(final List<TwoRoutesReplanner> replanners,
	// final double demand,
			final double capacity
	// , final Vector externalities
	) {
		this.replanners = replanners;
		// this.demand = demand;
		this.capacity = capacity;
		// this.externalities = externalities.copy();
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void implementTollsInSimulation(final Vector tolls) {
		this.tolls = tolls.copy();
	}

	void implementQ1sInSimulation(final Vector q1s) {
		for (int i = 0; i < this.replanners.size(); i++) {
			this.replanners.get(i).setQ1(q1s.get(i));
		}
	}

	// -------------------- INNER CLASSES --------------------

	// TODO move this to utils
	private static <T> T getWraparound(final List<T> data, int index) {
		while (index < 0) {
			index += data.size();
		}
		while (index >= data.size()) {
			index -= data.size();
		}
		return data.get(index);
	}

	// TODO move this to utils
	private static double getWraparound(final Vector data, int index) {
		return getWraparound(data.asList(), index);
	}

	private class InternalState implements SimulatorState {

		private final Vector q1s;
		private final Vector qs;
		private final Vector tts;
		private final Vector cs;

		private InternalState() {

			this.q1s = new Vector(replanners.size());
			this.qs = new Vector(replanners.size());
			for (int i = 0; i < replanners.size(); i++) {
				this.q1s.set(i, replanners.get(i).getRealizedQ1());
				this.qs.set(i, replanners.get(i).getRealizedQ1()
						+ getWraparound(replanners, i - 1).getRealizedQ2());
			}
			// this.q1s = new Vector(replanners.size());
			// this.qs = new Vector(replanners.size() + 1);
			// for (int i = 0; i < this.q1s.size(); i++) {
			// this.q1s.set(i, replanners.get(i).getRealizedQ1());
			// this.qs.set(i, replanners.get(i).getRealizedQ1());
			// }
			// for (int i = 1; i < this.qs.size(); i++) {
			// this.qs.add(i, replanners.get(i - 1).getRealizedQ2());
			// }

			this.tts = new Vector(replanners.size());
			for (int i = 0; i < replanners.size(); i++) {
				this.tts.set(i, Math.pow(this.qs.get(i) / capacity, 2.0));
			}

			this.cs = this.tts.copy();
			for (int i = 0; i < replanners.size(); i++) {
				this.cs.add(i, tolls.get(i));
			}
		}

		// private double equilibriumGap() {
		// double result = 0;
		// for (int i = 0; i < replanners.size(); i++) {
		// final double cMin = Math.min(this.cs.get(i),
		// getWraparound(this.cs, i + 1));
		// result = Math.max(result, this.q1s.get(i)
		// * (this.cs.get(i) - cMin) + (demand - this.q1s.get(i))
		// * (getWraparound(this.cs, i + 1) - cMin));
		// }
		// return result;
		// }

		@Override
		public Vector getReferenceToVectorRepresentation() {
			// return this.q1s.copy();
			// return this.qs.copy();
			return Vector.concat(this.qs, this.tts);
		}

		@Override
		public void implementInSimulation() {
			implementQ1sInSimulation(this.q1s);
		}

		// @Override
		// public double getObjectiveFunctionValue() {
		// double result = 0.0;
		// for (int i = 0; i < replanners.size(); i++) {
		// result += replanners.get(i).getRealizedQ1()
		// * (this.tts.get(i) + externalities.get(i));
		// result += replanners.get(i).getRealizedQ2()
		// * (getWraparound(this.tts, i + 1) + getWraparound(
		// externalities, i + 1));
		// }
		// return result;
		// }
	}

	// -------------------- IMPLEMENTATION OF System --------------------

	@Override
	public SimulatorState run(final TrajectorySampler<U> sampler) {
		return this.run(sampler, null);
	}

	// -------------------- SIMULATION LOGIC --------------------

	@Override
	public SimulatorState run(final TrajectorySampler<U> sampler,
			final SimulatorState initialState) {

		// for (int i = 0; i < this.replanners.size(); i++) {
		// if (initialQ1s != null) {
		// this.replanners.get(i).setQ1(initialQ1s.get(i));
		// } else {
		// this.replanners.get(i).setQ1(0.5 * this.demand);
		// }
		// }

		if (initialState != null) {
			initialState.implementInSimulation();
		}
		sampler.initialize();

		while (!sampler.foundSolution()) {

			final InternalState beforeChoice = new InternalState();
			for (int i = 0; i < this.replanners.size(); i++) {
				this.replanners.get(i).update(beforeChoice.cs.get(i),
						getWraparound(beforeChoice.cs, i + 1));
			}

			final InternalState afterChoice = new InternalState();
			sampler.afterIteration(afterChoice);
			// System.out.println("\t" + afterChoice.equilibriumGap());
		}

		return new InternalState();
	}

	final Vector finalQ1s() {
		return (new InternalState()).q1s.copy();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------
	//
	// public static void main(String[] args) {
	//
	// System.out.println("STARTED ...");
	//
	// final int twoRoutePairCnt = 100;
	//
	// final List<TwoRoutesReplanner> replanners = new
	// ArrayList<TwoRoutesReplanner>();
	// Vector externalities = new Vector(twoRoutePairCnt + 1);
	// Vector tolls = new Vector(twoRoutePairCnt + 1);
	// for (int i = 0; i < twoRoutePairCnt; i++) {
	// replanners.add(new ContinuousShiftToBestRouteReplanner(0.05 / Math
	// .pow(0.5 * DEMAND / CAPACITY, 2.0)));
	// externalities.set(i, (Math.pow(0.5 * DEMAND / CAPACITY, 2.0) * i)
	// / twoRoutePairCnt);
	// tolls.set(i, 0.0);
	// }
	// final NTimesTwoRoutes nTimesTwoRoutes = new NTimesTwoRoutes(replanners,
	// externalities);
	//
	// final Set<NTimesTwoRoutesDecisionVariable> decisionVariables = new
	// LinkedHashSet<NTimesTwoRoutesDecisionVariable>();
	// decisionVariables.add(new NTimesTwoRoutesDecisionVariable(
	// nTimesTwoRoutes, tolls));
	// final ConvergenceCriterion convergenceCriterion = new
	// FixedIterationNumberConvergenceCriterion(
	// 100, 10);
	// final SamplingStrategy samplingStrategy = new
	// OneTrajectorySamplingStrategy(
	// decisionVariables);
	// final Random rnd = new Random();
	// final TrajectorySampler trajectorySampler = new TrajectorySampler(
	// decisionVariables, convergenceCriterion, samplingStrategy, rnd);
	//
	// nTimesTwoRoutes.run(trajectorySampler, null);
	//
	// System.out.println("... DONE");
	// }
}
