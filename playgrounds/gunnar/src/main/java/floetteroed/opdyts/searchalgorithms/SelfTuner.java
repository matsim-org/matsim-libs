package floetteroed.opdyts.searchalgorithms;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.trajectorysampling.Transition;
import floetteroed.opdyts.trajectorysampling.TransitionSequencesAnalyzer;
import floetteroed.utilities.math.LeastAbsoluteDeviations;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SelfTuner {

	// -------------------- MEMBERS --------------------

	private final double memory;

	private final double convergenceThreshold;

	private final int maxIts;

	private final double initialPointWeight;

	private final double equivalentAveragingIterations;

	private final LinkedList<Double> equilibriumGaps = new LinkedList<>();

	private final LinkedList<Double> uniformityGaps = new LinkedList<>();

	private final LinkedList<Double> meanObjFctVals = new LinkedList<>();

	private final LinkedList<Double> finalObjFctVals = new LinkedList<>();

	private double equilibriumGapWeight = 0.0;

	private double uniformityGapWeight = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public SelfTuner(final double memory, final double convergenceThreshold,
			final int maxIts, final double initialPointWeight,
			final double equivalentAveragingIterations) {
		this.memory = memory;
		this.convergenceThreshold = convergenceThreshold;
		this.maxIts = maxIts;
		this.initialPointWeight = initialPointWeight;
		this.equivalentAveragingIterations = equivalentAveragingIterations;
	}

	// public SelfTuner() {
	// this(1.0, 0.01, 10, 0.01);
	// }

	// -------------------- INTERNALS --------------------

	private double average(final List<Double> values, final Double oneMoreValue) {
		double sum = oneMoreValue;
		for (Double value : values) {
			sum += value;
		}
		return sum / (1.0 + values.size());
	}

	// -------------------- GETTERS --------------------

	public double getEquilibriumGapWeight() {
		return this.equilibriumGapWeight;
	}

	public double getUniformityGapWeight() {
		return this.uniformityGapWeight;
	}

	// -------------------- IMPLEMENTATION --------------------

	private int its = 0;

	public <U extends DecisionVariable> void update(
			final List<Transition<U>> transitions,
			final ConvergenceCriterionResult convCritResult) {

		final double lastFinalObjFctVal = convCritResult.finalObjectiveFunctionValue;

		double lastEquilibriumGap;
		double lastUniformityGap;
		double lastMeanObjFctVal;

		double deltaEquilibriumGapWeight;
		double deltaUniformityGapWeight;

		int it = 0;
		do {
			it++;
			Logger.getLogger(this.getClass().getName()).info(
					"updating weights, round " + it + ", equilGapWeight="
							+ this.equilibriumGapWeight + ", unifGapWeight="
							+ this.uniformityGapWeight);

			final TransitionSequencesAnalyzer<U> analyzer = new TransitionSequencesAnalyzer<>(
					transitions, this.equilibriumGapWeight,
					this.uniformityGapWeight,
					this.equivalentAveragingIterations);
			final Vector alphas = analyzer.optimalAlphas(null); // TODO
			lastEquilibriumGap = analyzer.equilibriumGap(alphas);
			lastUniformityGap = alphas.innerProd(alphas);
			lastMeanObjFctVal = analyzer.originalObjectiveFunctionValue(alphas);

			final LeastAbsoluteDeviations lad = new LeastAbsoluteDeviations();
			lad.setLowerBounds(0.0, Double.NEGATIVE_INFINITY);
			// an initial measurement equation pulling towards zero weights
			lad.add(new Vector(this.initialPointWeight
					* this.average(this.equilibriumGaps, lastEquilibriumGap),
					this.initialPointWeight * 1.0), 0.0);
			// the most recent measurement equation
			lad.add(new Vector(lastEquilibriumGap, 1.0), lastFinalObjFctVal
					- lastMeanObjFctVal);
			// all previous measurement equations
			for (int i = 1; i < this.equilibriumGaps.size(); i++) {
				final double weight = Math.pow(this.memory, i);
				lad.add(new Vector(weight * this.equilibriumGaps.get(i),
						weight * 1.0),
						weight
								* (this.finalObjFctVals.get(i) - this.meanObjFctVals
										.get(i)));
			}

			try {
				lad.solve();
			} catch (Exception e) {
				Logger.getLogger(this.getClass().getName())
						.warn(e.getMessage());
			}

			final Vector newCoeffs = lad.getCoefficients();
			final double innoWeight = 1.0 / it;
			final double newEquilibriumGapWeight = innoWeight
					* newCoeffs.get(0) + (1.0 - innoWeight)
					* this.equilibriumGapWeight;
			final double newUniformityGapWeight = 0.0;
			deltaEquilibriumGapWeight = newEquilibriumGapWeight
					- this.equilibriumGapWeight;
			deltaUniformityGapWeight = newUniformityGapWeight
					- this.uniformityGapWeight;
			this.equilibriumGapWeight = newEquilibriumGapWeight;
			this.uniformityGapWeight = newUniformityGapWeight;

			Logger.getLogger(this.getClass().getName()).info(
					"v=" + this.equilibriumGapWeight + ", w="
							+ this.uniformityGapWeight);
		} while ((it < this.maxIts)
				&& ((Math.abs(deltaEquilibriumGapWeight) > this.convergenceThreshold
						* this.equilibriumGapWeight) || (Math
						.abs(deltaUniformityGapWeight) > this.convergenceThreshold
						* this.uniformityGapWeight)));

		this.equilibriumGaps.addFirst(lastEquilibriumGap);
		this.uniformityGaps.addFirst(lastUniformityGap);
		this.meanObjFctVals.addFirst(lastMeanObjFctVal);
		this.finalObjFctVals.addFirst(lastFinalObjFctVal);

		// /////////////////////////////////////////////////////////////

		// System.out.println("weight\tweightedgap\tsurr\texact\texactlower\texactupper");
		// for (double testEquilGapWeight = 0; testEquilGapWeight <= 10.0;
		// testEquilGapWeight += 0.5) {
		// final TransitionSequencesAnalyzer<U> analyzer = new
		// TransitionSequencesAnalyzer<>(
		// transitions, testEquilGapWeight, 0.0,
		// this.equivalentAveragingIterations);
		// final Vector alphas = analyzer.optimalAlphas(null);
		// System.out
		// .println(testEquilGapWeight
		// + "\t"
		// + testEquilGapWeight
		// * analyzer.equilibriumGap(alphas)
		// + "\t"
		// + analyzer.surrogateObjectiveFunctionValue(alphas)
		// + "\t"
		// + convCritResult.finalObjectiveFunctionValue
		// + "\t"
		// + (convCritResult.finalObjectiveFunctionValue -
		// convCritResult.finalObjectiveFunctionValueStddev)
		// + "\t"
		// + (convCritResult.finalObjectiveFunctionValue +
		// convCritResult.finalObjectiveFunctionValueStddev));
		// }
		// System.exit(0);

		// //////////////////////////////////////////////////////////////

		// this.its++;
		// final double innoWeight = 1.0 / this.its;
		//
		// final double newEquilibriumGapWeight =
		// convCritResult.finalObjectiveFunctionValue - convCritResult.;
		// this.equilibriumGapWeight = innoWeight * newEquilibriumGapWeight +
		// (1.0 - innoWeight) * this.equilibriumGapWeight;
		// this.uniformityGapWeight = 0.0;

		// /////////////////////////////////////////////////////

		// final double f = 2.0;
		// this.equilibriumGapWeight = f
		// * convCritResult.finalObjectiveFunctionValueStddev
		// / (2.0 * convCritResult.finalEquilibriumGap);
		// this.uniformityGapWeight = f
		// * convCritResult.finalObjectiveFunctionValueStddev
		// / (2.0 * convCritResult.finalUniformityGap);

		// ////////////////////////////////////////////////////////////////

		// final double lastFinalObjFctVal =
		// convCritResult.finalObjectiveFunctionValue;
		//
		// double lastEquilibriumGap;
		// double lastUniformityGap;
		// double lastMeanObjFctVal;
		//
		// double deltaEquilibriumGapWeight;
		// double deltaUniformityGapWeight;
		//
		// int it = 0;
		// do {
		// it++;
		// Logger.getLogger(this.getClass().getName()).info(
		// "updating weights, round " + it + ", equilGapWeight="
		// + this.equilibriumGapWeight + ", unifGapWeight="
		// + this.uniformityGapWeight);
		//
		// final TransitionSequencesAnalyzer<U> analyzer = new
		// TransitionSequencesAnalyzer<>(
		// transitions, this.equilibriumGapWeight,
		// this.uniformityGapWeight,
		// this.equivalentAveragingIterations);
		// final Vector alphas = analyzer.optimalAlphas(null); // TODO
		// lastEquilibriumGap = analyzer.equilibriumGap(alphas);
		// lastUniformityGap = alphas.innerProd(alphas);
		// lastMeanObjFctVal = analyzer.originalObjectiveFunctionValue(alphas);
		//
		// final LeastAbsoluteDeviations lad = new LeastAbsoluteDeviations();
		// lad.setLowerBounds(0.0, 0.0);
		// // an initial measurement equation pulling towards zero weights
		// lad.add(new Vector(this.initialPointWeight
		// * this.average(this.equilibriumGaps, lastEquilibriumGap),
		// this.initialPointWeight
		// * this.average(this.uniformityGaps,
		// lastUniformityGap)), 0.0);
		// // the most recent measurement equation
		// lad.add(new Vector(lastEquilibriumGap, lastUniformityGap),
		// lastFinalObjFctVal - lastMeanObjFctVal);
		// // all previous measurement equations
		// for (int i = 1; i < this.equilibriumGaps.size(); i++) {
		// final double weight = Math.pow(this.memory, i);
		// lad.add(new Vector(weight * this.equilibriumGaps.get(i), weight
		// * this.uniformityGaps.get(i)),
		// weight
		// * (this.finalObjFctVals.get(i) - this.meanObjFctVals
		// .get(i)));
		// }
		//
		// try {
		// lad.solve();
		// } catch (Exception e) {
		// Logger.getLogger(this.getClass().getName())
		// .warn(e.getMessage());
		// }
		//
		// final Vector newCoeffs = lad.getCoefficients();
		// final double innoWeight = 1.0 / it;
		// final double newEquilibriumGapWeight = innoWeight
		// * newCoeffs.get(0) + (1.0 - innoWeight)
		// * this.equilibriumGapWeight;
		// final double newUniformityGapWeight = innoWeight * newCoeffs.get(1)
		// + (1.0 - innoWeight) * this.uniformityGapWeight;
		// deltaEquilibriumGapWeight = newEquilibriumGapWeight
		// - this.equilibriumGapWeight;
		// deltaUniformityGapWeight = newUniformityGapWeight
		// - this.uniformityGapWeight;
		// this.equilibriumGapWeight = newEquilibriumGapWeight;
		// this.uniformityGapWeight = newUniformityGapWeight;
		//
		// Logger.getLogger(this.getClass().getName()).info(
		// "v=" + this.equilibriumGapWeight + ", w="
		// + this.uniformityGapWeight);
		// } while ((it < this.maxIts)
		// && ((Math.abs(deltaEquilibriumGapWeight) > this.convergenceThreshold
		// * this.equilibriumGapWeight) || (Math
		// .abs(deltaUniformityGapWeight) > this.convergenceThreshold
		// * this.uniformityGapWeight)));
		//
		// this.equilibriumGaps.addFirst(lastEquilibriumGap);
		// this.uniformityGaps.addFirst(lastUniformityGap);
		// this.meanObjFctVals.addFirst(lastMeanObjFctVal);
		// this.finalObjFctVals.addFirst(lastFinalObjFctVal);
	}
}
