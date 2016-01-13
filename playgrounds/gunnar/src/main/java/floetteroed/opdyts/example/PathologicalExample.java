package floetteroed.opdyts.example;

import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PathologicalExample {

	private final LinearSystemSimulator system;

	private final String logFileName;

	private final int populationSize;

	public PathologicalExample(final int populationSize,
			final String logFileName, final Random rnd) {
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
		this.system = new LinearSystemSimulator(_A, _B, 0.0, rnd);
		this.logFileName = logFileName;
		this.populationSize = populationSize;
	}

	public void run() {

		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				100, 1);
		final ObjectiveFunction objFct = new LinearSystemObjectiveFunction();

		final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;
		final boolean keepBestSolution = true;
		final boolean interpolate = true;
		final int maxRandomSearchIterations = 50;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		// final int randomSearchPopulationSize = 14;
		final Double inertia = null; // null triggers MSA

		final RandomSearch<VectorDecisionVariable> randomSearch = new RandomSearch<>(
				system, new VectorDecisionVariableRandomizer(2, 0.1,
						MatsimRandom.getRandom(), this.system, new Vector(-0.1,
								-0.1), new Vector(0.1, 0.1)),
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, this.populationSize,
				MatsimRandom.getRandom(), interpolate, keepBestSolution,
				objFct, maxMemorizedTrajectoryLength, inertia);
		randomSearch.setLogFileName(this.logFileName);

		randomSearch.run();
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final int populationSize = 14;
		final PathologicalExample example = new PathologicalExample(
				populationSize, "./small-system_pop-size-" + populationSize
						+ ".log", new Random());
		example.run();

		System.out.println("... DONE.");
	}
}
