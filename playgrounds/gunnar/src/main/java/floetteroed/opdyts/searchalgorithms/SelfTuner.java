package floetteroed.opdyts.searchalgorithms;

import static java.lang.Math.abs;

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

	final double inertia;

	// most recent ones first
	private final LinkedList<List<Double>> equililibriumGaps = new LinkedList<>();

	// most recent ones first
	private final LinkedList<List<Double>> uniformityGaps = new LinkedList<>();

	// most recent ones first
	private final LinkedList<List<Double>> avgObjFctVals = new LinkedList<>();

	// most recent ones first
	private final LinkedList<Double> finalObjFctVals = new LinkedList<>();

	private double equilibriumGapWeight = 0.0;

	private double uniformityGapWeight = 0.0;

	private boolean noisySystem = false;

	// -------------------- CONSTRUCTION --------------------

	public SelfTuner(final double inertia) {
		this.inertia = Math.sqrt(inertia);
	}

	public void setNoisySystem(final boolean noisySystem) {
		this.noisySystem = noisySystem;
	}

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
			final List<Double> avgObjFctVals, final double finalObjFctVal) {
		for (int k = 0; k < avgObjFctVals.size(); k++) {
			final Vector x = new Vector(regr.getDimension());
			{
				int i = 0;
				if (equilGaps != null) {
					x.set(i++, this.inertia * equilGaps.get(k));
				}
				if (unifGaps != null) {
					x.set(i, this.inertia * unifGaps.get(k));
				}
			}
			regr.update(x, this.inertia
					* abs(avgObjFctVals.get(k) - finalObjFctVal));
		}
	}

	private void fit(final boolean useEquilGap, final boolean useUnifGap) {
		int dim = (useEquilGap ? 1 : 0);
		if (useUnifGap) {
			dim++;
		}
		final Regression regr = new Regression(1.0, dim);
		for (int i = 0; i < this.equililibriumGaps.size(); i++) {
			this.addMeasurements(regr,
					(useEquilGap ? this.equililibriumGaps.get(i) : null),
					(useUnifGap ? this.uniformityGaps.get(i) : null),
					this.avgObjFctVals.get(i), this.finalObjFctVals.get(i));
		}
		int i = 0;
		if (useEquilGap) {
			this.equilibriumGapWeight = regr.getCoefficients().get(i++);
		} else {
			this.equilibriumGapWeight = 0.0;
		}
		if (useUnifGap) {
			this.uniformityGapWeight = regr.getCoefficients().get(i);
		} else {
			this.uniformityGapWeight = 0.0;
		}
	}

	public <U extends DecisionVariable> void update(
			final List<SamplingStage<U>> stages, final double newFinalObjFctVal) {

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

		if (this.noisySystem) {
			this.fit(true, true);
			if ((this.equilibriumGapWeight < 0.0)) {
				if (this.uniformityGapWeight > 0.0) {
					this.fit(false, true);
				} else {
					// both non-positive, constrain to (0, 0)
				}
			} else {
				if (this.uniformityGapWeight < 0.0) {
					this.fit(true, false);
				} else {
					// both non-negative, no constraint needed
				}
			}
		} else {
			this.fit(true, false);
		}
		this.equilibriumGapWeight = Math.max(0.0, this.equilibriumGapWeight);
		this.uniformityGapWeight = Math.max(0.0, this.uniformityGapWeight);

		Logger.getLogger(this.getClass().getName()).info(
				"v=" + this.equilibriumGapWeight + ", w="
						+ this.uniformityGapWeight);
	}
}
