package floetteroed.opdyts.convergencecriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import floetteroed.opdyts.trajectorysampling.TransitionSequence;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SomewhatCleverConvergenceCriterion implements ConvergenceCriterion {

	// -------------------- CONSTANTS --------------------

	private final int iterationsToConvergence;

	// -------------------- CONSTRUCTION --------------------

	public SomewhatCleverConvergenceCriterion(final int iterationsToConvergence) {
		this.iterationsToConvergence = iterationsToConvergence;
	}

	// -------------------- INTERNALS --------------------

	private void enumeratePossibilities(final List<Double> allObjFctVals) {
		for (int avgIts = allObjFctVals.size() - 1; avgIts >= 1; avgIts--) {
			final BasicStatistics candObjFctValStats = new BasicStatistics(
					allObjFctVals.subList(allObjFctVals.size() - avgIts,
							allObjFctVals.size()));
			final double varOfTheVar = 2.0
					* Math.pow(candObjFctValStats.getVar(), 2.0)
					/ (candObjFctValStats.size() - 1);
			System.out.print(candObjFctValStats.getVar()
					+ 2.0 * Math.sqrt(varOfTheVar));
			System.out.print("\t");
		}
		System.out.println();
	}

	private BasicStatistics bestObjFctValStats(final List<Double> allObjFctVals) {
		BasicStatistics bestObjFctValStats = new BasicStatistics(allObjFctVals);
		for (int avgIts = allObjFctVals.size() - 1; avgIts >= 1; avgIts--) {
			final BasicStatistics candObjFctValStats = new BasicStatistics(
					allObjFctVals.subList(allObjFctVals.size() - avgIts,
							allObjFctVals.size()));
			if (candObjFctValStats.getVar() <= bestObjFctValStats.getVar()) {
				bestObjFctValStats = candObjFctValStats;
			} else {
				break;
			}
		}
		return bestObjFctValStats;
	}

	// --------------- IMPLEMENTATION OF ConvergenceCriterion ---------------

	@Override
	public ConvergenceCriterionResult evaluate(
			final TransitionSequence<?> transitionSequence) {

		if ((transitionSequence.iterations() < this.iterationsToConvergence)) {

			return null;

		} else {

			// objective function statistics
			final BasicStatistics bestObjFctValStats = new BasicStatistics(
					transitionSequence.getObjectiveFunctionValues());

			// gap statistics
			final Vector totalDelta = transitionSequence.getTransitions()
					.get(transitionSequence.size() - bestObjFctValStats.size())
					.getDelta().copy();
			for (int i = transitionSequence.size() - bestObjFctValStats.size()
					+ 1; i < transitionSequence.size(); i++) {
				totalDelta.add(transitionSequence.getTransitions().get(i)
						.getDelta());
			}

			// package the results
			return new ConvergenceCriterionResult(bestObjFctValStats.getAvg(),
					bestObjFctValStats.getStddev(), totalDelta.euclNorm()
							/ bestObjFctValStats.size(),
					1.0 / bestObjFctValStats.size(),
					transitionSequence.getDecisionVariable(),
					transitionSequence.size());
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final Random rnd = new Random();
		final int maxIt = 100;
		for (double var : new double[] { 0.0, 1e-4, 1e-3, 1e-2, 1e-1, 1e0 }) {
			final List<Double> values = new ArrayList<>(100);
			for (int i = 0; i < maxIt; i++) {
				values.add(Math.exp(-0.05 * i) + Math.sqrt(var)
						* rnd.nextGaussian());
			}
			final SomewhatCleverConvergenceCriterion crit = new SomewhatCleverConvergenceCriterion(
					maxIt);
			System.out.print("var=" + var + "\t");
			crit.enumeratePossibilities(values);
		}
	}

}
