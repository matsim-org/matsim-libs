package besttimeresponse;

import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TestTimeAllocation {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		PlannedActivity home = PlannedActivity.newOvernightActivity("home", "car", 8 * 3600);
		PlannedActivity work = PlannedActivity.newWithinDayActivity("office", "car", 8 * 3600, 6 * 3600, 18 * 3600,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
		PlannedActivity shop = PlannedActivity.newWithinDayActivity("store", "car", 1 * 3600, 8 * 3600, 21 * 3600,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

		TimeDiscretization discr = new TimeDiscretization(0, 3600, 24);
		RealizedActivitiesBuilder builder = new RealizedActivitiesBuilder(discr, new TravelTimes() {
			@Override
			public double getTravelTime_s(double dptTime_s, Object mode) {
				return 1800;
			}
		});
		builder.addActivity(home);
		builder.addTrip(7 * 3600);
		builder.addActivity(work);
		builder.addTrip(15 * 3600);
		builder.addActivity(shop);
		builder.addTrip(16 * 3600);
		final List<RealizedActivity> acts = builder.getResult();

		double betaDur_1_s = 1.0;
		double betaTravel_1_s = 0.0;
		double betaLateArr_1_s = 0.0;
		double betaEarlyDpt_1_s = 0.0;
		LinearTimeAllocationProblem problem = new LinearTimeAllocationProblem(acts, betaDur_1_s, betaTravel_1_s,
				betaLateArr_1_s, betaEarlyDpt_1_s);

		System.out.println("dS/dDptTimes = " + problem.get__dScore_dDptTimes__1_s());

		LinearObjectiveFunction objFct = problem.getObjectiveFunction();
		LinearConstraintSet constr = problem.getConstraints();
		System.out.println("objFct = " + objFct);
		System.out.println("constr = " + constr);
		SimplexSolver opt = new SimplexSolver();
		PointValuePair result = opt.optimize(problem.getObjectiveFunction(), problem.getConstraints(),
				GoalType.MAXIMIZE);
		System.out.println(new ArrayRealVector(result.getPoint()));

		System.out.println("... DONE.");
	}

}
