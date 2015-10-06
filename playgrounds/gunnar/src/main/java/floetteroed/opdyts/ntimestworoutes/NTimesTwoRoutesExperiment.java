package floetteroed.opdyts.ntimestworoutes;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.TrajectorySamplingSelfTuner;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesExperiment {

	// static final double DEMAND = 1000;

	// static final double CAPACITY = 1000;

	static enum SAMPLING {
		full_interpol, single_interpol, single
	};

	// -------------------- MEMBERS --------------------

	private final double demand;

	private final double capacity;

	private final List<TwoRoutesReplanner> replanners;

	private final Vector externalities;

	final ConvergenceCriterion convergenceCriterion;

	final Random rnd;

	final SAMPLING sampling;

	// -------------------- CONSTRUCTION --------------------

	NTimesTwoRoutesExperiment(final double demand, final double capacity,
			final List<TwoRoutesReplanner> replanners,
			final Vector externalities,
			final ConvergenceCriterion convergenceCriterion,
			final SAMPLING sampling, final Random rnd) {

		this.demand = demand;
		this.capacity = capacity;

		this.replanners = Collections.unmodifiableList(new ArrayList<>(
				replanners));
		this.externalities = externalities.copy();
		this.convergenceCriterion = convergenceCriterion;
		this.sampling = sampling;
		this.rnd = rnd;
	}

	// -------------------- EXPERIMENTS --------------------

	// static Vector initialP1s(final Vector etas,
	// final Set<NTimesTwoRoutesDecisionVariable> decisionVariables,
	// final List<TwoRoutesReplanner> replanners,
	// final ConvergenceCriterion convergenceCriterion, final Random rnd) {
	//
	// final Vector avgThetas = new Vector(etas.size());
	// for (NTimesTwoRoutesDecisionVariable u : decisionVariables) {
	// avgThetas.add(u.getTolls());
	// }
	// avgThetas.mult(1.0 / decisionVariables.size());
	//
	// final NTimesTwoRoutes nTimesTwoRoutes = new NTimesTwoRoutes(replanners,
	// DEMAND, CAPACITY, etas);
	//
	// final SamplingStrategy samplingStrategy = new
	// OneTrajectorySamplingStrategy(
	// new NTimesTwoRoutesDecisionVariable(nTimesTwoRoutes, avgThetas));
	//
	// final TrajectorySampler trajectorySampler = new TrajectorySampler(
	// decisionVariables, convergenceCriterion, samplingStrategy, rnd);
	//
	// nTimesTwoRoutes.run(trajectorySampler, null);
	// return nTimesTwoRoutes.finalP1s();
	// }

	void run(final Set<Vector> candidateTolls, final Vector initialQ1s,
			final int replications, final String logFileName,
			final TrajectorySamplingSelfTuner selfTuner)
			throws FileNotFoundException {

		System.out.println(logFileName);
		final PrintWriter writer = new PrintWriter(logFileName);
		writer.println("theta" + "\t" + "final_obj" + "\t" + "trans_cnt" + "\t"
				+ "equil_weight" + "\t" + "unif_weight" + "\t" + "offset"
				+ "\t" + "offset2" + "\t" + "objectiveFunctionWeight");
		writer.flush();

		// double equilibriumGapWeight = initialEquilibriumGapWeight;
		// double uniformityWeight = initialUniformityWeight;
		// double performanceOffset = initialPerformanceOffset;
		// double interpolatedObjectiveFunctionWeight =
		// initialInterpolatedObjectiveFunctionWeight;

		// final TrajectorySamplingSelfTuner selfTuner = new
		// TrajectorySamplingSelfTuner();

		for (int r = 1; r <= replications; r++) {

			System.out.println(r + " of " + replications);

			final NTimesTwoRoutesSimulator system = new NTimesTwoRoutesSimulator(
					this.replanners, this.demand, this.capacity,
					this.externalities);
			final Set<NTimesTwoRoutesDecisionVariable> decisionVariables = new LinkedHashSet<>();
			for (Vector toll : candidateTolls) {
				decisionVariables.add(new NTimesTwoRoutesDecisionVariable(
						system, toll));
			}

			// final SamplingStrategy samplingStrategy;
			// if (SAMPLING.full_interpol.equals(this.sampling)) {
			// samplingStrategy = new FullInterpolationSamplingStrategy(1.0,
			// selfTuner.getEquilibriumGapWeight(),
			// selfTuner.getUniformityWeight(), null /* TODO */);
			// } else if (SAMPLING.single_interpol.equals(this.sampling)) {
			// samplingStrategy = new InterpolationPerDecisionVariableSampling(
			// selfTuner.getEquilibriumGapWeight(),
			// selfTuner.getUniformityWeight(), false); // TODO
			// throw new RuntimeException("TODO");
			// } else if (SAMPLING.single.equals(this.sampling)) {
			// samplingStrategy = new OneTrajectorySamplingStrategy(
			// decisionVariables); // TODO
			// throw new RuntimeException("TODO");
			// } else {
			// samplingStrategy = null;
			// }
			//
			// final TrajectorySampler trajectorySampler = new
			// TrajectorySampler(
			// decisionVariables, null, samplingStrategy,
			// this.convergenceCriterion, this.rnd);
			// system.run(trajectorySampler, null);
			//
			// final DecisionVariable finalDecisionVariable = trajectorySampler
			// .getConvergedDecisionVariables().iterator().next();
			// final double finalObjectiveFunctionValue = trajectorySampler
			// .getFinalObjectiveFunctionValue(finalDecisionVariable);
			//
			// try {
			// selfTuner.registerSamplingStageSequence(
			// trajectorySampler.getSamplingStages(),
			// finalObjectiveFunctionValue,
			// trajectorySampler.getInitialGradientNorm(), null);
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			// if (r > preparatoryIterations) {
			// interpolatedObjectiveFunctionWeight = selfTuner
			// .getInterpolatedObjectiveFunctionWeight();
			// equilibriumGapWeight = selfTuner.getEquilibriumGapWeight();
			// uniformityWeight = selfTuner.getUniformityWeight();
			// performanceOffset = selfTuner.getOffset();
			// }

//			writer.println(finalDecisionVariable + "\t"
//					+ finalObjectiveFunctionValue + "\t"
//					+ trajectorySampler.getTotalTransitionCnt() + "\t"
//					+ selfTuner.getEquilibriumGapWeight() + "\t"
//					+ selfTuner.getUniformityWeight() + "\t"
//					+ selfTuner.getOffset() + "\t" + 1.0);
//			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	// ==================================================================
	// =============== CONCRETE EXPERIMENTS STARTING HERE ===============
	// ==================================================================

	static void exp1() throws FileNotFoundException {

		final Random rnd = new Random();

		final double demand = 1000;
		final double capacity = 1000;

		final int replications = 1;
		final String logFileName = "exp1.log";

		final double deltaCostAtWhichSwitchingIsCertain = Math.pow(demand
				/ capacity, 2.0);
		final int twoRoutePairCnt = 1;
		final List<TwoRoutesReplanner> replanners = new ArrayList<TwoRoutesReplanner>(
				twoRoutePairCnt);
		for (int i = 0; i < twoRoutePairCnt; i++) {
			// replanners.add(new ContinuousShiftToBestRouteReplanner(DEMAND,
			// 10 * deltaCostAtWhichSwitchingIsCertain));
			final IndividualDecisionsRouteReplanner replanner = new IndividualDecisionsRouteReplanner(
					MathHelpers.round(demand), true, rnd);
			// replanner.setDeltaCostAtWhichSwitchingIsCertain(10 * Math.pow(
			// DEMAND / CAPACITY, 2));
			replanner.setFixedReplanningProbability(0.1);
			replanners.add(replanner);
		}
		// final ConvergenceCriterion convergenceCriterion = new
		// ObjectiveFunctionChangeConvergenceCriterion(
		// 1e-8, 1e-8, 1);
		final ConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
				1e-6, 1e-6, 5);

		final Vector initialQ1s = new Vector(twoRoutePairCnt);
		initialQ1s.fill(1.0 * demand);

		final Vector externalities = new Vector(twoRoutePairCnt + 1);
		final Vector thetas = new Vector(twoRoutePairCnt + 1);
		for (int i = 0; i < twoRoutePairCnt + 1; i++) {
			externalities.set(i, (Math.pow(0.5 * demand / capacity, 2.0) * i)
					/ twoRoutePairCnt);
			thetas.set(i, 0.0);
		}

		final NTimesTwoRoutesExperiment exp = new NTimesTwoRoutesExperiment(
				demand, capacity, replanners, externalities,
				convergenceCriterion, SAMPLING.full_interpol, rnd);

		// final double initialEquilibriumGapWeight = 0.0;
		// final double initialUniformityWeight = 0.0;
		// final double initialPerformanceOffset = 0.0;
		// final double initialInterpolatedObjectiveFunctionWeight = 1.0;
		// final int preparatoryIterations = 8;

		final TrajectorySamplingSelfTuner selfTuner = null;
		// new TrajectorySamplingSelfTuner(
		// 0.0, 0.0, 0.0, 8);

		exp.run(new LinkedHashSet<>(Arrays.asList(new Vector[] { thetas })),
				initialQ1s, replications, logFileName, selfTuner
		// initialEquilibriumGapWeight, initialUniformityWeight,
		// initialPerformanceOffset,
		// initialInterpolatedObjectiveFunctionWeight,
		// preparatoryIterations
		);
	}

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("STARTED");
		exp1();
		System.out.println("DONE");
	}
}
