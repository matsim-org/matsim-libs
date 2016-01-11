package floetteroed.opdyts.example;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.math.Matrix;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RunPathologicalExample {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		/*
		 * Problem specification.
		 */
		final Matrix _A = new Matrix(2, 2);
		_A.getRow(0).set(0, 0.0);
		_A.getRow(0).set(1, 0.9);
		_A.getRow(1).set(0, 0.9);
		_A.getRow(1).set(1, 0.0);
		final Matrix _B = new Matrix(2, 2);
		_B.getRow(0).set(0, 1.0);
		_B.getRow(0).set(1, 0.0);
		_B.getRow(1).set(0, -3.2);
		_B.getRow(1).set(1, 1.0);
		final LinearSystemSimulator system = new LinearSystemSimulator(_A, _B);

		final int maxSimulationIterations = 100;
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				maxSimulationIterations, 1);
		
		final ObjectiveFunction objFct = new LinearSystemObjectiveFunction();

		/*
		 * RandomSearch specification.
		 */
		final int maxMemorizedTrajectoryLength = maxSimulationIterations;
		final boolean keepBestSolution = true;
		final boolean interpolate = true;
		final int maxRandomSearchIterations = 1000;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final int randomSearchPopulationSize = 15;

		final RandomSearch<VectorDecisionVariable> randomSearch = new RandomSearch<>(
				system, new VectorDecisionVariableRandomizer(2, 0.1,
						MatsimRandom.getRandom(), system),
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, randomSearchPopulationSize,
				MatsimRandom.getRandom(), interpolate, keepBestSolution,
				objFct, maxMemorizedTrajectoryLength);

		/*
		 * Run it.
		 */
		randomSearch.run();

		System.out.println("... DONE.");
	}
}
