package floetteroed.opdyts.example.pathological;

import java.io.IOException;
import java.util.Random;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
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

	private final String convFileName;

	private final String outerLogFileName;

	private final int populationSize;

	private final boolean naive;

	public PathologicalExample(final int populationSize,
			final String logFileName, final String convFileName,
			final String outerLogFileName, final Random rnd, final boolean naive) {
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
		final double sigmaNoise = 0.0;
		this.system = new LinearSystemSimulator(_A, _B, sigmaNoise, rnd);
		this.logFileName = logFileName;
		this.convFileName = convFileName;
		this.outerLogFileName = outerLogFileName;
		this.populationSize = populationSize;
		this.naive = naive;
	}

	public void run() {

		final Random rnd = new Random();

		final int maxIts = 100;
		final int avgIts = 1;
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				maxIts, avgIts);
		final ObjectiveFunction objFct = new LinearSystemObjectiveFunction(1.0,
				0.0);

		final boolean interpolate = true;
		final int maxRandomSearchIterations = 10;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final boolean includeCurrentBest = false;

		final Vector min = new Vector(Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY); // new Vector(-0.1, -0.1);
		final Vector max = new Vector(Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY); // new Vector(0.1, 0.1);

		final RandomSearch<VectorDecisionVariable> randomSearch = new RandomSearch<>(
				system, new VectorDecisionVariableRandomizer(2, 0.1, rnd,
						this.system, min, max), new VectorDecisionVariable(
						new Vector(2), this.system), convergenceCriterion,
				maxRandomSearchIterations, maxRandomSearchTransitions,
				this.populationSize, rnd, interpolate, objFct,
				includeCurrentBest);
		randomSearch.setLogFileName(this.logFileName);
		randomSearch.setConvergenceTrackingFileName(this.convFileName);
		randomSearch.setOuterIterationLogFileName(this.outerLogFileName);
		
		randomSearch.setMaintainAllTrajectories(true);
		randomSearch.setMaxTotalMemory(Integer.MAX_VALUE);

		if (this.naive) {
			randomSearch.setMaxMemoryPerTrajectory(1);
			randomSearch.run(0.0, 0.0);
		} else {
			randomSearch.setMaxMemoryPerTrajectory(1);
			// randomSearch.setMaxMemoryPerTrajectory(Integer.MAX_VALUE);
			final SelfTuner selfTuner = new SelfTuner(0.95);
			selfTuner.setNoisySystem(false);
			randomSearch.run(selfTuner);
		}
	}

	public static void main(String[] args) throws IOException {

		// System.err.println("MAKE SURE THAT DATA IS SECURED");
		// System.exit(-1);

		final boolean naive = true;
		final String path = "./output/pathological/" + "test/";
		// + (naive ? "naive/" : "proposed/");
//		for (int populationSize : new int[] { 2, 4, 8, 16, 32, 64, 128, 256 }) {
//			for (int seed : new int[] { 1000, 2000, 3000, 4000, 5000, 6000,
//					7000, 8000, 9000, 10000 }) {
		for (int populationSize : new int[] { 2 }) {
			for (int seed : new int[] { 2000 }) {
				final Random rnd = new Random(seed);
				final String logFileName = path + "popSize" + populationSize
						+ "_seed" + seed + ".log";
				final String convFileName = path + "popSize" + populationSize
						+ "_seed" + seed + ".conv";
				final String outerLogFileName = path + "popSize"
						+ populationSize + "_seed" + seed + ".opt";
				new PathologicalExample(populationSize, logFileName,
						convFileName, outerLogFileName, rnd, naive).run();
			}
		}
	}

	public static final String logFileNameFromParams(final String path,
			final int populationSize, final int seed) {
		return (path + "popSize" + populationSize + "_seed" + seed + ".log");
	}
}
