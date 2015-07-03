package tworoutes;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import optdyts.DecisionVariableSetEvaluator;
import optdyts.logging.SearchStatisticsWriter;

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

	private final double replanningFraction;

	private final int maxIterations;

	// -------------------- MEMBERS --------------------

	// the decision variable
	private double theta;

	// the state variable
	private int flow1;

	// -------------------- CONSTRUCTION --------------------

	TwoRoutes(final int totalDemand, final double capacity,
			final double replanningFraction, final int maxIterations) {
		this.totalDemand = totalDemand;
		this.capacity = capacity;
		this.replanningFraction = replanningFraction;
		this.maxIterations = maxIterations;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void setToll(final double toll) {
		this.theta = toll;
	}

	void setFlow1(final int flow1) {
		this.flow1 = flow1;
	}

	// -------------------- SIMULATION LOGIC --------------------

	void run(
			final DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> evaluator) {

		evaluator.initialize();

		this.flow1 = this.totalDemand / 2;

		System.out.println("tt1\ttt2\tflow1\tflow2");

		for (int it = 1; it <= this.maxIterations; it++) {

			final double tt1 = Math.pow(this.flow1 / this.capacity, 2.0);
			final double tt2 = Math.pow((this.totalDemand - this.flow1)
					/ this.capacity, 2.0);

			if (tt1 + this.theta > tt2) {
				// route 1 is more expensive
				int shift = 0;
				for (int n = 0; n < this.flow1; n++) {
					if (this.rnd.nextDouble() < this.replanningFraction) {
						shift++;
					}
				}
				this.flow1 -= shift;
			} else {
				// route 2 is more expensive
				int shift = 0;
				for (int n = this.flow1; n < this.totalDemand; n++) {
					if (this.rnd.nextDouble() < this.replanningFraction) {
						shift++;
					}
				}
				this.flow1 += shift;
			}

			System.out.println(tt1 + "\t" + tt2 + "\t" + this.flow1 + "\t"
					+ (this.totalDemand - this.flow1));

			final TwoRoutesSimulatorState newState = new TwoRoutesSimulatorState(
					this, this.flow1, this.totalDemand - this.flow1, tt1, tt2);
			evaluator.afterIteration(newState);

		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {

		final int totalDemand = 1000;
		final double capacity = 750;
		final double replanningFraction = 0.05;
		final int maxIterations = 100;
		final TwoRoutes twoRoutes = new TwoRoutes(totalDemand, capacity,
				replanningFraction, maxIterations);

		final double maxTheta = Math.pow(totalDemand / capacity, 2.0);

		final Set<TwoRoutesDecisionVariable> decisionVariables = new LinkedHashSet<>();
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.0 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.1 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.2 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.3 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.4 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.5 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.6 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.7 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.8 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				0.9 * maxTheta));
		decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
				1.0 * maxTheta));
		final double eta = 2.66;
		final TwoRoutesObjectiveFunction objectiveFunction = new TwoRoutesObjectiveFunction(
				eta);
		final int minimumAverageIterations = 5;
		final double maximumRelativeGap = 0.05;
		final DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> evaluator = new DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable>(
				decisionVariables, objectiveFunction, minimumAverageIterations,
				maximumRelativeGap);
		evaluator.setStandardLogFileName("twoRoutes.log");

		final SearchStatisticsWriter<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> averageWriter = new SearchStatisticsWriter<TwoRoutesSimulatorState, TwoRoutesDecisionVariable>(
				"twoRoutesAvg.txt");
		averageWriter.addSearchStatistic(new TwoRoutesAverageToll(
				decisionVariables));
		evaluator.addSearchStatisticsWriter(averageWriter);

		twoRoutes.run(evaluator);
	}
}
