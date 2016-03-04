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

	final double measWeightFact;

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

	// -------------------- CONSTRUCTION --------------------

	public SelfTuner(final double inertia) {
		this.measWeightFact = Math.sqrt(inertia);
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
					x.set(i++, this.measWeightFact * equilGaps.get(k));
				}
				if (unifGaps != null) {
					x.set(i, this.measWeightFact * unifGaps.get(k));
				}
			}
			regr.update(x, this.measWeightFact
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
		}
		if (useUnifGap) {
			this.uniformityGapWeight = regr.getCoefficients().get(i);
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

		this.fit(true, true);
		if (this.equilibriumGapWeight < 0.0) {
			this.equilibriumGapWeight = 0.0;
			if (this.uniformityGapWeight < 0.0) {
				this.uniformityGapWeight = 0.0;
			} else {
				this.fit(false, true);
				this.uniformityGapWeight = Math.max(0.0,
						this.uniformityGapWeight);
			}
		} else {
			if (this.uniformityGapWeight < 0.0) {
				this.fit(true, false);
				this.uniformityGapWeight = Math.max(0.0,
						this.uniformityGapWeight);
			} else {
				// nothing to do, all weights non-negative
			}
		}

		Logger.getLogger(this.getClass().getName()).info(
				"v=" + this.equilibriumGapWeight + ", w="
						+ this.uniformityGapWeight);
	}
}
