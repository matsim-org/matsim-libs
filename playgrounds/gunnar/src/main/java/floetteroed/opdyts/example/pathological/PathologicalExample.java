package floetteroed.opdyts.example.pathological;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.analysis.LogFileAnalyzer;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.latex.PSTricksDiagramWriter;
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
		// _A.getRow(0).set(0, 0.9);
		// _A.getRow(0).set(1, 0.0);
		// _A.getRow(1).set(0, 0.0);
		// _A.getRow(1).set(1, 0.9);
		final Matrix _B = new Matrix(2, 2);
		_B.getRow(0).set(0, 1.0);
		_B.getRow(0).set(1, 0.0);
		_B.getRow(1).set(0, -3.2);
		_B.getRow(1).set(1, 1.0);
		this.system = new LinearSystemSimulator(_A, _B, 0.1, rnd);
		this.logFileName = logFileName;
		this.populationSize = populationSize;
	}

	public void run() {

		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				150, 50);
		final ObjectiveFunction objFct = new LinearSystemObjectiveFunction();

		final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;
		final boolean interpolate = true;
		final int maxRandomSearchIterations = 10;
		final int maxRandomSearchTransitions = Integer.MAX_VALUE;
		final boolean includeCurrentBest = true;
		
		final RandomSearch<VectorDecisionVariable> randomSearch = new RandomSearch<>(
				system, new VectorDecisionVariableRandomizer(2, 0.1,
						MatsimRandom.getRandom(), this.system, new Vector(-0.1,
								-0.1), new Vector(0.1, 0.1)),
				new VectorDecisionVariable(new Vector(2), this.system),
				convergenceCriterion, maxRandomSearchIterations,
				maxRandomSearchTransitions, this.populationSize,
				MatsimRandom.getRandom(), interpolate, objFct,
				maxMemorizedTrajectoryLength, includeCurrentBest);
		randomSearch.setLogFileName(this.logFileName);

		randomSearch.run();
	}

	public static void main(String[] args) throws IOException {

		final Random rnd = new Random(4711);

		for (int populationSize : new int[] { 16 }) {

			final PSTricksDiagramWriter writer = new PSTricksDiagramWriter(8.0,
					6.0);
			writer.setEndLine("\n");
			writer.setLabelX("transitions [$10^3$]");
			writer.setLabelY("$Q_\\text{surr} - Q_\\text{final}$");
			writer.setXMin(0.0);
			writer.setXMax(10.0);
			writer.setXDelta(1.0);
			writer.setYMin(-1.0);
			writer.setYMax(+1.0);
			writer.setYDelta(0.1);
			writer.setPlotAttrs("data", "plotstyle=dots");

			final int maxRepl = 1;
			for (int repl = 1; repl <= maxRepl; repl++) {
				final String logFileName = "./small-system_pop-size-"
						+ populationSize + "_repl-" + repl + ".log";
				new PathologicalExample(populationSize, logFileName, rnd).run();
				final LogFileAnalyzer lfa = new LogFileAnalyzer(logFileName);
				final List<Integer> its = lfa.getTotalTransitions();
				final List<Double> gaps = lfa.getObjectiveFunctionValueGaps();
				for (int i = 0; i < its.size(); i++) {
					writer.add("data", its.get(i) / 1000.0, gaps.get(i));
				}
			}

			writer.printAll(System.out);
		}
	}
}
