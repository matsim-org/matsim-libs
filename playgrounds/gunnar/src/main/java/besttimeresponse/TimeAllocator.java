package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.utilities.Units;
import floetteroed.utilities.searchrepeater.SearchAlgorithm;
import floetteroed.utilities.searchrepeater.SearchRepeater;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 */
public class TimeAllocator<L, M> {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscretization;

	private final TripTravelTimes<L, M> travelTimes;

	private final boolean repairTimeStructure;

	private final boolean randomSmoothing;

	private final boolean interpolate;

	private final boolean verbose = false;

	// -------------------- CONSTRUCTION --------------------

	public TimeAllocator(final TimeDiscretization timeDiscretization, final TripTravelTimes<L, M> travelTimes,
			final boolean repairTimeStructure, final boolean randomSmoothing, final boolean interpolate) {
		this.timeDiscretization = timeDiscretization;
		this.travelTimes = travelTimes;
		this.repairTimeStructure = repairTimeStructure;
		this.randomSmoothing = randomSmoothing;
		this.interpolate = interpolate;
	}

	// -------------------- INTERNALS --------------------

	private TimeAllocationProblem newTimeAllocationProblem(final List<PlannedActivity<L, M>> plannedActivities,
			double[] initialDptTimes_s) {
		if (initialDptTimes_s == null) {
			initialDptTimes_s = new double[plannedActivities.size()];
			for (int q = 0; q < initialDptTimes_s.length; q++) {
				initialDptTimes_s[q] = MatsimRandom.getRandom().nextDouble() * Units.S_PER_D;
			}
			Arrays.sort(initialDptTimes_s);
		}
		final RealizedActivitiesBuilder<L, M> builder = new RealizedActivitiesBuilder<L, M>(this.timeDiscretization,
				this.travelTimes, this.repairTimeStructure);
		for (int q = 0; q < plannedActivities.size(); q++) {
			builder.addActivity(plannedActivities.get(q), initialDptTimes_s[q]);
		}
		builder.build();
		return new TimeAllocationProblem(builder.getResult());
	}

	// -------------------- IMPLEMENTATION --------------------

	public double evaluate(final List<PlannedActivity<L, M>> plannedActivities, final double[] initialDptTimes_s) {
		if (initialDptTimes_s == null) {
			throw new RuntimeException("Cannot evaluate a plan without time structure.");
		}
		TimeAllocationProblem problem = this.newTimeAllocationProblem(plannedActivities, initialDptTimes_s);
		return problem.getTimeScoreAtInitialSolution();
	}

	private double[] currentDptTimes_s = null;

	private Double currentScore = null;

	public double[] getResultPoint() {
		return this.currentDptTimes_s;
	}

	public Double getResultValue() {
		return this.currentScore;
	}

	public double[] optimizeDepartureTimes(final List<PlannedActivity<L, M>> plannedActivities,
			final double[] initialDptTimes_s) {

		TimeAllocationProblem problem = this.newTimeAllocationProblem(plannedActivities, initialDptTimes_s);
		this.currentDptTimes_s = problem.getInitialSolution();
		this.currentScore = problem.getTimeScoreAtInitialSolution();
		RealVector currentGradient = problem.get__dScore_dDptTimes__1_s();

		if (this.verbose) {
			System.out.println("---");
		}
		
		while (true) {

			if (this.verbose) {
				System.out.println("current dpt. times: " + new ArrayRealVector(this.currentDptTimes_s) + ", score = "
						+ this.currentScore);
			}

			double[] newDptTimes_s = (new SimplexSolver())
					.optimize(problem.getObjectiveFunction(), problem.getConstraints(), GoalType.MAXIMIZE).getPoint();
			problem = this.newTimeAllocationProblem(plannedActivities, newDptTimes_s);
			final double newScore = problem.getTimeScoreAtInitialSolution();
			final RealVector newGradient = problem.get__dScore_dDptTimes__1_s();

			if (newScore <= this.currentScore) {

				if (this.interpolate) {

					final RealVector deltaDptTime_s = new ArrayRealVector(newDptTimes_s)
							.subtract(new ArrayRealVector(this.currentDptTimes_s));
					final double deltaDptTimeNorm_s = deltaDptTime_s.getNorm();

					if (deltaDptTimeNorm_s >= 1e-8) {
						final double g0 = deltaDptTime_s.dotProduct(currentGradient) / deltaDptTimeNorm_s;
						final double g1 = deltaDptTime_s.dotProduct(newGradient) / deltaDptTimeNorm_s;
						if ((g0 > 0) && (g1 < 0)) {
							final double _Q0 = this.currentScore;
							final double _Q1 = newScore;

							final double eta = max(0, min(1.0, 0.5 - (_Q1 - _Q0) / (g1 - g0)));
							/*
							 * The above line search expression works as
							 * follows.
							 * 
							 * _Q0 and _Q1 are the objective function values at
							 * eta = 0 and eta = 1, respectively. g0 and g1 are
							 * the gradients (more specifically, the gradient
							 * projections onto the line search direction) at
							 * eta = 0 and eta = 1, respectively.
							 * 
							 * The value range is such that eta = 0 falls back
							 * to the previous solution, eta = 1 takes over the
							 * new solution, and otherwise an interpolation
							 * takes place.
							 * 
							 * The line search assumes that g0 > 0 and g1 < 0.
							 * One hence has (g1 - g0) < 0. The line search uses
							 * the expression
							 * 
							 * 0.5 - (_Q1 - _Q0) / (g1 - g0)
							 * 
							 * = 0.5 + (_Q1 - _Q0) * (some positive constant).
							 * 
							 * If _Q0 = _Q1, then both extreme points are
							 * equally good and the line search returns their
							 * average 0.5.
							 * 
							 * If _Q1 > _Q0, then the new solution is better and
							 * the line search returns a value > 0.5, i.e.
							 * closer to the new solution.
							 *
							 * If _Q0 > _Q1, then the old solution is better and
							 * the line search returns a value < 0.5, i.e.
							 * closer to the old solution.
							 * 
							 * In addition, if the objective function is
							 * quadratic then the line search returns its
							 * *exact* maximum.
							 */

							final ArrayRealVector interpolDptTime_s = new ArrayRealVector(this.currentDptTimes_s)
									.combine(1.0, eta, deltaDptTime_s);
							for (int q = 0; q < interpolDptTime_s.getDimension(); q++) {
								this.currentDptTimes_s[q] = interpolDptTime_s.getEntry(q);
							}
							if (this.verbose) {
								System.out
										.println("current dpt. times: " + new ArrayRealVector(this.currentDptTimes_s)
												+ ", score = "
												+ this.newTimeAllocationProblem(plannedActivities,
														this.currentDptTimes_s).getObjectiveFunction()
														.value(this.currentDptTimes_s));
							}
						}
					}
				}

				if (this.randomSmoothing) {
					for (int q = 0; q < this.currentDptTimes_s.length; q++) {
						this.currentDptTimes_s[q] += this.timeDiscretization.getBinSize_s()
								* (MatsimRandom.getRandom().nextDouble() - 0.5);
					}
				}

				if (this.verbose) {
					System.out.println("---");
				}

				return this.currentDptTimes_s;
			}

			this.currentDptTimes_s = newDptTimes_s;
			this.currentScore = newScore;
			currentGradient = newGradient;
		}
	}

	public void optimizeDepartureTimes(final List<PlannedActivity<L, M>> plannedActivities, final int maxTrials,
			final int maxFailures) {

		// for referencing in inner class
		final TimeAllocator<L, M> enclosingTimeAllocator = this;

		final SearchRepeater<double[]> repeater = new SearchRepeater<>(maxTrials, maxFailures);
		repeater.setMaximize();
		repeater.run(new SearchAlgorithm<double[]>() {
			private double[] departureTimes = null;
			private Double timeScore = null;

			@Override
			public void run() {
				this.departureTimes = enclosingTimeAllocator.optimizeDepartureTimes(plannedActivities, null);
				this.timeScore = enclosingTimeAllocator.getResultValue();
			}

			@Override
			public Double getObjectiveFunctionValue() {
				return this.timeScore;
			}

			@Override
			public double[] getDecisionVariable() {
				return this.departureTimes;
			}
		});

		this.currentDptTimes_s = repeater.getDecisionVariable();
		this.currentScore = repeater.getObjectiveFunctionValue();
	}
}
