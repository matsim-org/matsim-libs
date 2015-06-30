package tworoutes;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import optdyts.algorithms.DecisionVariableSetEvaluator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutes {

	// -------------------- CONSTANTS --------------------

	private final Random rnd = new Random();

	private final int totalDemand;

	private final double capacity;

	private final int maxIterations;

	// -------------------- MEMBERS --------------------

	// the decision variable
	private double toll;

	// the state variable
	private double flow1;

	// -------------------- CONSTRUCTION --------------------

	TwoRoutes(final int totalDemand, final double capacity,
			final int maxIterations) {
		this.totalDemand = totalDemand;
		this.capacity = capacity;
		this.maxIterations = maxIterations;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void setToll(final double toll) {
		this.toll = toll;
	}

	void setFlow1(final double flow1) {
		this.flow1 = flow1;
	}

	// -------------------- SIMULATION LOGIC --------------------

	void updateFlow1(final double tt1, final double tt2) {
		final double vot = 1.0;
		this.flow1 = 0;
		double v1 = -vot * tt1 - this.toll;
		double v2 = -vot * tt2;
		double vOffset = -Math.max(v1, v2);
		final double p1 = Math.exp(v1 + vOffset)
				/ (Math.exp(v1 + vOffset) + Math.exp(v2 + vOffset));
		for (int n = 0; n < this.totalDemand; n++) {
			if (this.rnd.nextDouble() < p1) {
				this.flow1++;
			}
		}
	}

	void run(
			final DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> evaluator) {

		evaluator.initialize();

		double tt1 = 0;
		double tt2 = 0;
		updateFlow1(tt1, tt2);

		System.out.println("tt1\ttt2\tflow1\tflow2");

		for (int it = 1; it <= this.maxIterations; it++) {

			tt1 = Math.pow(this.flow1 / this.capacity, 2.0);
			tt2 = Math
					.pow((this.totalDemand - this.flow1) / this.capacity, 2.0);

			updateFlow1(tt1, tt2);

			System.out.println(tt1 + "\t" + tt2 + "\t" + this.flow1 + "\t"
					+ (this.totalDemand - this.flow1));

			final TwoRoutesSimulatorState newState = new TwoRoutesSimulatorState(
					this, this.flow1, tt1 + tt2, this.toll);
			evaluator.afterIteration(newState);

		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {

		final int totalDemand = 1000;
		final double capacity = 750;
		final int maxIterations = 100;
		final TwoRoutes twoRoutes = new TwoRoutes(totalDemand, capacity,
				maxIterations);

		final Set<TwoRoutesDecisionVariable> decisionVariables = new LinkedHashSet<>();
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.0));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.1));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.2));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.3));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.4));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.5));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.6));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.7));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.8));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 0.9));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes, 1.0));
		final double damage1 = -0.05;
		final TwoRoutesObjectiveFunction objectiveFunction = new TwoRoutesObjectiveFunction(
				damage1);
		final int minimumAverageIterations = 5;
		final double maximumRelativeGap = 0.1;
		final DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> evaluator = new DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable>(
				decisionVariables, objectiveFunction, minimumAverageIterations,
				maximumRelativeGap);
		evaluator.setLogFileName("twoRoutes.log");

		twoRoutes.run(evaluator);
	}
}
