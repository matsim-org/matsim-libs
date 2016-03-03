package floetteroed.opdyts.searchalgorithms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SelfTuner {

	// -------------------- MEMBERS --------------------

	// private final double memory;

	private final double convergenceThreshold;

	private final int maxIts;

	// private final double initialPointWeight;
	//
	// private final LinkedList<Double> equilibriumGaps = new LinkedList<>();
	//
	// private final LinkedList<Double> uniformityGaps = new LinkedList<>();
	//
	// private final LinkedList<Double> meanObjFctVals = new LinkedList<>();

	private final LinkedList<List<Double>> equililibriumGaps = new LinkedList<>();

	private final LinkedList<List<Double>> uniformityGaps = new LinkedList<>();

	private final LinkedList<List<Double>> avgObjFctVals = new LinkedList<>();

	private final LinkedList<Double> finalObjFctVals = new LinkedList<>();

	private double equilibriumGapWeight = 0.0;

	private double uniformityGapWeight = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public SelfTuner(final double memory, final double convergenceThreshold,
			final int maxIts, final double initialPointWeight) {
		// this.memory = memory;
		this.convergenceThreshold = convergenceThreshold;
		this.maxIts = maxIts;
		// this.initialPointWeight = initialPointWeight;
	}

	public SelfTuner() {
		this(1.0, 0.01, 10, 0.01);
	}

	// -------------------- INTERNALS --------------------

	// private double average(final List<Double> values, final Double
	// oneMoreValue) {
	// double sum = oneMoreValue;
	// for (Double value : values) {
	// sum += value;
	// }
	// return sum / (1.0 + values.size());
	// }

	// -------------------- GETTERS --------------------

	public double getEquilibriumGapWeight() {
		return this.equilibriumGapWeight;
	}

	public double getUniformityGapWeight() {
		return this.uniformityGapWeight;
	}

	// -------------------- IMPLEMENTATION --------------------

	private void addMeasurements(final Regression regr,
			final List<Double> equilGaps, final List<Double> unifGaps,
			final List<Double> avgObjFctVals, final double finalObjFctVal,
			final double weight) {
		for (int k = 0; k < equilGaps.size(); k++) {
			regr.update(
					new Vector(weight * equilGaps.get(k), weight
							* unifGaps.get(k)),
					weight * Math.abs(avgObjFctVals.get(k) - finalObjFctVal));
		}
	}

	public <U extends DecisionVariable> void update(
			final List<SamplingStage<U>> stages,
			// final List<Transition<U>> transitions,
			final double newFinalObjFctVal) {

		final List<Double> newEquilibriumGaps = new ArrayList<>(stages.size());
		final List<Double> newUniformityGaps = new ArrayList<>(stages.size());
		final List<Double> newAvgObjFctVals = new ArrayList<>(stages.size());
		for (int k = 0; k < stages.size(); k++) {
			newEquilibriumGaps.add(stages.get(k).getEquilibriumGap());
			newUniformityGaps.add(stages.get(k).getUniformityGap());
			newAvgObjFctVals.add(stages.get(k)
					.getOriginalObjectiveFunctionValue());
		}
		this.equililibriumGaps.addFirst(newEquilibriumGaps);
		this.uniformityGaps.addFirst(newUniformityGaps);
		this.avgObjFctVals.addFirst(newAvgObjFctVals);
		this.finalObjFctVals.addFirst(newFinalObjFctVal);

		final Regression regr = new Regression(1.0, 2);
		for (int i = 0; i < this.equililibriumGaps.size(); i++) {
			final double weight = Math.pow(Math.sqrt(0.95), i);
			this.addMeasurements(regr, this.equililibriumGaps.get(i),
					this.uniformityGaps.get(i), this.avgObjFctVals.get(i),
					this.finalObjFctVals.get(i), weight);
		}

		final Vector newCoeffs = regr.getCoefficients();
		final double innoWeight = 1.0;
		final double newEquilibriumGapWeight = innoWeight
				* Math.max(newCoeffs.get(0), 0.0) + (1.0 - innoWeight)
				* this.equilibriumGapWeight;
		final double newUniformityGapWeight = innoWeight
				* Math.max(newCoeffs.get(1), 0.0) + (1.0 - innoWeight)
				* this.uniformityGapWeight;
		this.equilibriumGapWeight = newEquilibriumGapWeight;
		this.uniformityGapWeight = newUniformityGapWeight;

		Logger.getLogger(this.getClass().getName()).info(
				"v=" + this.equilibriumGapWeight + ", w="
						+ this.uniformityGapWeight);

	}
}
