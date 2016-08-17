package besttimeresponse;

import java.util.List;

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

	public static void main(String[] args) {

		// System.out.println("STARTED ...");

		int dptFromHome_s = Time.secFromStr("07:00:00");
		int dptFromWork_s = Time.secFromStr("11:00:00");
		int dptFromShop_s = Time.secFromStr("20:00:00");

		for (int it = 0; it < 100; it++) {

			PlannedActivity home = PlannedActivity.newOvernightActivity("home", "car", 8.0 * 3600, null, null);
			PlannedActivity work = PlannedActivity.newWithinDayActivity("office", "car", 8.0 * 3600, 6.0 * 3600,
					18.0 * 3600, null, null);
			PlannedActivity shop = PlannedActivity.newWithinDayActivity("store", "car", 1.0 * 3600, 8.0 * 3600,
					21.0 * 3600, null, null);

			TimeDiscretization discr = new TimeDiscretization(0, 3600, 24);
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

			double betaDur_1_s = 1.0;
			double betaTravel_1_s = -1.0;
			double betaLateArr_1_s = -1.0;
			double betaEarlyDpt_1_s = -1.0;
			LinearTimeAllocationProblem problem = new LinearTimeAllocationProblem(acts, betaDur_1_s, betaTravel_1_s,
					betaLateArr_1_s, betaEarlyDpt_1_s);

			// System.out.println("dS/dDptTimes = " +
			// problem.get__dScore_dDptTimes__1_s());
			// for (LinearConstraint constr :
			// problem.getConstraints().getConstraints()) {
			// System.out.println("constraint: " + constr.getCoefficients() +
			// constr.getRelationship()
			// + constr.getValue() + "(" + Time.strFromSec((int)
			// constr.getValue()) + ")");
			// }
			SimplexSolver opt = new SimplexSolver();
			PointValuePair result = opt.optimize(problem.getObjectiveFunction(), problem.getConstraints(),
					GoalType.MAXIMIZE);
			for (double dptTime_s : result.getPoint()) {
				// System.out.print(Time.strFromSec((int) dptTime_s) + "\t");
				System.out.print(dptTime_s + "\t");
			}
			System.out.println();
			dptFromHome_s = (int) result.getPoint()[0];
			dptFromWork_s = (int) result.getPoint()[1];
			dptFromShop_s = (int) result.getPoint()[2];

		}

		// System.out.println("... DONE.");
	}

}
