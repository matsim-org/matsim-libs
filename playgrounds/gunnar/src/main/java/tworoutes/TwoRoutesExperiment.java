package tworoutes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import optdyts.DecisionVariableSetEvaluator;

public class TwoRoutesExperiment {

	// -------------------- MEMBERS --------------------

	private final int totalDemand;

	private final double capacity;

	private final double eta;

	private final int replications;

	private final int minimumAverageIterations;

	private final double maximumRelativeGap;

	private final int maxIterations;

	private final int memory;

	private final double thetaMin;

	private final double thetaIncrement;

	private final double thetaCount;

	private final double replanningFraction;

	private final boolean terminateWhenSolutionFound;

	// MEMBERS

	private final List<Double> averageMinusOptimalSolution = new ArrayList<Double>();

	private final List<Double> exactMinusOptimalSolution = new ArrayList<Double>();

	// -------------------- CONSTRUCTION --------------------

	public TwoRoutesExperiment(final int totalDemand, final double capacity,
			final double eta, final int replications,
			final int minimumAverageIterations,
			final double maximumRelativeGap, final int maxIterations,
			final int memory, final double thetaMin, final double thetaMax,
			final double thetaCount, final double replanningFraction,
			final boolean terminateWhenSolutionFound) {
		this.totalDemand = totalDemand;
		this.capacity = capacity;
		this.eta = eta;
		this.replications = replications;
		this.minimumAverageIterations = minimumAverageIterations;
		this.maximumRelativeGap = maximumRelativeGap;
		this.maxIterations = maxIterations;
		this.memory = memory;
		this.thetaMin = thetaMin;
		this.thetaCount = thetaCount;
		this.thetaIncrement = (thetaMax - thetaMin) / (thetaCount - 1);
		this.replanningFraction = replanningFraction;
		this.terminateWhenSolutionFound = terminateWhenSolutionFound;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() {

		for (int r = 0; r < this.replications; r++) {

			// Prepare a single experiment.

			final TwoRoutes twoRoutes = new TwoRoutes(this.totalDemand,
					this.capacity, this.replanningFraction, this.maxIterations,
					this.terminateWhenSolutionFound);

			final Set<TwoRoutesDecisionVariable> decisionVariables = new LinkedHashSet<>();
			for (int i = 0; i < this.thetaCount; i++) {
				decisionVariables.add(new TwoRoutesDecisionVariable(twoRoutes,
						this.thetaMin + i * this.thetaIncrement));
			}

			final TwoRoutesObjectiveFunction objectiveFunction = new TwoRoutesObjectiveFunction(
					this.eta);
			final DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> evaluator = new DecisionVariableSetEvaluator<TwoRoutesSimulatorState, TwoRoutesDecisionVariable>(
					decisionVariables, objectiveFunction,
					this.minimumAverageIterations, this.maximumRelativeGap);

			twoRoutes.run(evaluator);

			// Analyze results of that experiment.

			final TwoRoutesAnalytical twoRoutesAnalytical = new TwoRoutesAnalytical(
					this.totalDemand, this.capacity);
			final double thetaOpt = twoRoutesAnalytical.thetaOpt(this.eta);

			if (evaluator.foundSolution()) {
				this.exactMinusOptimalSolution.add(thetaOpt
						- evaluator.getCurrentDecisionVariable().getTheta());
			} else {
				final TwoRoutesAverageToll averageToll = new TwoRoutesAverageToll();
				this.exactMinusOptimalSolution.add(thetaOpt
						- averageToll.numericalValue(evaluator
								.getCurrentSurrogateSolution()));
			}
		}
	}

	public static void main(String[] args) {
		
		final int totalDemand = 1000;
		final double capacity = 750;
		final double eta = 1.5 * Math.pow(totalDemand / capacity, 2.0);		
		
		final boolean terminateWhenSolutionFound = true;		
		final int replications = 1000;
		final double thetaCount = 11;
		
		final double replanningFraction;
		final int minimumAverageIterations;
		final double maximumRelativeGap;		
		final int memory;
		
		final double thetaMin; 
		final double thetaMax;
		
		
	}
}
