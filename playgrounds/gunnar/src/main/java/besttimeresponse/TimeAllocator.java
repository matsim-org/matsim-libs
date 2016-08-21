package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeAllocator {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscretization;

	private final TravelTimes travelTimes;

	private final boolean repairTimeStructure = true;

	private final double betaDur_1_s;

	private final double betaTravel_1_s;

	private final double betaLateArr_1_s;

	private final double betaEarlyDpt_1_s;

	// -------------------- CONSTRUCTION --------------------

	public TimeAllocator(final TimeDiscretization timeDiscretization, final TravelTimes travelTimes,
			final double betaDur_1_s, final double betaTravel_1_s, final double betaLateArr_1_s,
			final double betaEarlyDpt_1_s) {
		this.timeDiscretization = timeDiscretization;
		this.travelTimes = travelTimes;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
	}

	// -------------------- INTERNALS --------------------

	private TimeAllocationProblem newTimeAllocationProblem(final List<PlannedActivity> plannedActivities,
			final double[] dptTimes_s) {
		RealizedActivitiesBuilder builder = new RealizedActivitiesBuilder(this.timeDiscretization, this.travelTimes,
				this.repairTimeStructure);
		for (int q = 0; q < plannedActivities.size(); q++) {
			builder.addActivity(plannedActivities.get(q), dptTimes_s[q]);
		}
		builder.build();
		return new TimeAllocationProblem(builder.getResult(), this.betaDur_1_s, this.betaTravel_1_s,
				this.betaLateArr_1_s, this.betaEarlyDpt_1_s);
	}

	// -------------------- IMPLEMENTATION --------------------

	public double[] optimizeDepartureTimes(final List<PlannedActivity> plannedActivities,
			final double[] initialDptTimes_s) {

		double[] currentDptTimes_s = Arrays.copyOf(initialDptTimes_s, initialDptTimes_s.length);
		TimeAllocationProblem problem = this.newTimeAllocationProblem(plannedActivities, currentDptTimes_s);
		LinearObjectiveFunction objFct = problem.getObjectiveFunction();
		double currentScore = objFct.value(currentDptTimes_s);
		RealVector currentGradient = objFct.getCoefficients();

		while (true) {

			System.out.println(
					"current dpt. times: " + new ArrayRealVector(currentDptTimes_s) + ", score = " + currentScore);

			final double[] newDptTimes_s = (new SimplexSolver())
					.optimize(problem.getObjectiveFunction(), problem.getConstraints(), GoalType.MAXIMIZE).getPoint();

			problem = this.newTimeAllocationProblem(plannedActivities, newDptTimes_s);
			objFct = problem.getObjectiveFunction();
			final double newScore = objFct.value(newDptTimes_s);
			final RealVector newGradient = objFct.getCoefficients();

			if (newScore <= currentScore) {

				final RealVector deltaDptTime_s = new ArrayRealVector(newDptTimes_s)
						.subtract(new ArrayRealVector(currentDptTimes_s));
				final double deltaDptTimeNorm_s = deltaDptTime_s.getNorm();

				if (deltaDptTimeNorm_s >= 1e-8) {
					final double g0 = deltaDptTime_s.dotProduct(currentGradient) / deltaDptTimeNorm_s;
					final double g1 = deltaDptTime_s.dotProduct(newGradient) / deltaDptTimeNorm_s;
					if (g0 > 0 && g1 < 0) {
						final double _Q0 = currentScore;
						final double _Q1 = newScore;

						/*
						 * The line search works as follows.
						 * 
						 * The value range is such that eta = 0 falls back to
						 * the previous solution, eta = 1 takes over new
						 * solution, and otherwise an interpolation takes place.
						 * 
						 * The line search assumes that g0 > 0 and g1 < 0. One
						 * hence has (g1 - g0) < 0. The line search uses the
						 * expression
						 * 
						 * 0.5 - (_Q1 - _Q0) / (g1 - g0),
						 * 
						 * which then becomes
						 * 
						 * 0.5 + (_Q1 - _Q0) * (some positive constant).
						 * 
						 * If _Q0 = _Q1, then both extreme points are equally
						 * good and the line search returns their average 0.5.
						 * 
						 * If _Q1 > _Q0, then the new solution is better and the
						 * line search returns a value > 0.5, i.e. closed to the
						 * new solution.
						 *
						 * If _Q0 > _Q1, then the old solution is better and the
						 * line search returns a value < 0.5, i.e. closer to the
						 * old solution.
						 * 
						 * In addition, if the objective function is truly
						 * quadratic then the line search returns its *exact*
						 * maximum.
						 * 
						 */
						final double eta = max(0, min(1.0, 0.5 - (_Q1 - _Q0) / (g1 - g0)));

						final ArrayRealVector interpolDptTime_s = new ArrayRealVector(currentDptTimes_s).combine(1.0,
								eta, deltaDptTime_s);
						for (int q = 0; q < interpolDptTime_s.getDimension(); q++) {
							currentDptTimes_s[q] = interpolDptTime_s.getEntry(q);
						}
						System.out.println("current dpt. times: " + new ArrayRealVector(currentDptTimes_s)
								+ ", score = " + this.newTimeAllocationProblem(plannedActivities, currentDptTimes_s)
										.getObjectiveFunction().value(currentDptTimes_s));
					}
				}

				return currentDptTimes_s;
			}

			currentDptTimes_s = newDptTimes_s;
			currentScore = newScore;
			currentGradient = newGradient;
		}
	}
}
