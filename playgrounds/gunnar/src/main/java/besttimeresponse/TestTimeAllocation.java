package besttimeresponse;

import static floetteroed.utilities.math.MathHelpers.round;

import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import floetteroed.utilities.Time;
import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TestTimeAllocation {

	final static TimeDiscretization discr = new TimeDiscretization(0, 3600, 24);

	final static PlannedActivity home = PlannedActivity.newOvernightActivity("home", "car", 8.0 * 3600, null,
			12.0 * 3600);
	final static PlannedActivity work = PlannedActivity.newWithinDayActivity("office", "car", 8.0 * 3600, 6.0 * 3600,
			18.0 * 3600, null, null);
	final static PlannedActivity shop = PlannedActivity.newWithinDayActivity("store", "car", 1.0 * 3600, 8.0 * 3600,
			21.0 * 3600, null, null);

	final static double betaDur_1_s = 1.0;
	final static double betaTravel_1_s = -1.0;
	final static double betaLateArr_1_s = -1.0;
	final static double betaEarlyDpt_1_s = -1.0;

	private static TimeAllocationProblem newProblem(final double dptFromHome_s, final double dptFromWork_s,
			final double dptFromShop_s) {
		RealizedActivitiesBuilder builder = new RealizedActivitiesBuilder(discr, new TravelTimes() {
			@Override
			public double getTravelTime_s(Object origin, Object destination, double dptTime_s, Object mode) {
				return 0.1 * (Units.S_PER_D - dptTime_s);
			}
		});
		builder.addActivity(home, dptFromHome_s);
		builder.addActivity(work, dptFromWork_s);
		builder.addActivity(shop, dptFromShop_s);
		builder.build();
		final List<RealizedActivity> acts = builder.getResult();
		final TimeAllocationProblem problem = new TimeAllocationProblem(acts, betaDur_1_s, betaTravel_1_s,
				betaLateArr_1_s, betaEarlyDpt_1_s);
		return problem;
	}

	public static void main(String[] args) {

		int dptFromHome_s = Time.secFromStr("07:00:00");
		int dptFromWork_s = Time.secFromStr("11:00:00");
		int dptFromShop_s = Time.secFromStr("20:00:00");

		Double eta = null;
		boolean converged = false;
		Double lastScore = null;
		RealVector lastGradient = null;
		PointValuePair lastSolution = null;

		for (int it = 0; it < 100; it++) {

			final TimeAllocationProblem problem = newProblem(dptFromHome_s, dptFromWork_s, dptFromShop_s);

			System.out.println("[prior to opt.]   dpt(home) = " + Time.strFromSec(dptFromHome_s) + ", dpt(work) = "
					+ Time.strFromSec(dptFromWork_s) + ", dpt(shop) = " + Time.strFromSec(dptFromShop_s) + ", score = "
					+ problem.getScore() + ", converged = " + converged + ", eta = " + eta);

			if (lastScore == null) {
				lastScore = problem.getScore();
				lastGradient = new ArrayRealVector(problem.get__dScore_dDptTimes__1_s());
				final double[] lastPoint = new double[] { dptFromHome_s, dptFromWork_s, dptFromShop_s };
				lastSolution = new PointValuePair(new double[] { dptFromHome_s, dptFromWork_s, dptFromShop_s },
						new ArrayRealVector(lastPoint).dotProduct(lastGradient));
			}

			if (converged) {
				break;
			}

			SimplexSolver opt = new SimplexSolver();
			PointValuePair solution = opt.optimize(problem.getObjectiveFunction(), problem.getConstraints(),
					GoalType.MAXIMIZE);
			
			// >>>>> CONVERGENCE CHECK >>>>>

			final double score;
			final RealVector gradient;
			{
				final TimeAllocationProblem newProblem = newProblem(solution.getPoint()[0], solution.getPoint()[1],
						solution.getPoint()[2]);
				score = newProblem.getScore();
				gradient = newProblem.get__dScore_dDptTimes__1_s();
			}

			converged = (lastScore >= score);
			if (!converged) {
				dptFromHome_s = (int) solution.getPoint()[0];
				dptFromWork_s = (int) solution.getPoint()[1];
				dptFromShop_s = (int) solution.getPoint()[2];
			} else {
				// System.out.println("------------------------------------------");
				// System.out.println("CONVERGENCE DETECTED");
				// System.out.println("previous gradient: " + lastGradient);
				// System.out.println("current gradient: " + gradient);
				// System.out.println("------------------------------------------");

				final RealVector deltaDptTime_s = new ArrayRealVector(solution.getPoint())
						.subtract(new ArrayRealVector(lastSolution.getPoint()));

				final double _Q0 = lastScore;
				final double _Q1 = score;
				final double g0 = deltaDptTime_s.dotProduct(lastGradient) / deltaDptTime_s.getNorm();
				final double g1 = deltaDptTime_s.dotProduct(gradient) / deltaDptTime_s.getNorm();

				// System.out.println("Q0 = " + _Q0 + ", Q1 = " + _Q1 + ", g0 =
				// " + g0 + ", g1 = " + g1);

				if (g0 > 0 && g1 < 0) {
					eta = Math.max(0, Math.min(1.0, 0.5 - (_Q1 - _Q0) / (g1 - g0)));
					final ArrayRealVector finalDptTime_s = new ArrayRealVector(lastSolution.getPoint()).combine(1, eta,
							deltaDptTime_s);
					dptFromHome_s = round(finalDptTime_s.getEntry(0));
					dptFromWork_s = round(finalDptTime_s.getEntry(1));
					dptFromShop_s = round(finalDptTime_s.getEntry(2));
				} else {
					dptFromHome_s = round(lastSolution.getPoint()[0]);
					dptFromWork_s = round(lastSolution.getPoint()[1]);
					dptFromShop_s = round(lastSolution.getPoint()[2]);
				}

			}

			lastScore = score;
			lastGradient = gradient;
			lastSolution = solution;

			// <<<<< CONVERGENCE CHECK <<<<<

		}
	}

}
