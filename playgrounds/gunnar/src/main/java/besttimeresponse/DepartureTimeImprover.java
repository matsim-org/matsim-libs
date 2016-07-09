package besttimeresponse;

import java.util.ArrayList;
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
public class DepartureTimeImprover {

	// -------------------- MEMBERS --------------------

	private final double slack_s;

	private final InterpolatedTravelTimes travelTimes;

	private final double betaDur_1_s;

	private final double betaTravel_1_s;

	private final double betaLateArr_1_s;

	private final double betaEarlyDpt_1_s;

	private final double betaWait_1_s;

	private final List<Trip> trips = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public DepartureTimeImprover(final InterpolatedTravelTimes travelTimes, final double betaDur_1_s,
			final double betaTravel_1_s, final double betaLateArr_1_s, final double betaEarlyDpt_1_s,
			final double betaWait_1_s, final double slack_s) {
		this.travelTimes = travelTimes;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
		this.betaWait_1_s = betaWait_1_s;
		this.slack_s = slack_s;
	}

	// -------------------- HELPERS --------------------

	private LinearTimeAllocationProblem newLinearProblem(final RealVector departureTimes_s) {
		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			this.trips.get(tripIndex).departureTime_s = departureTimes_s.getEntry(tripIndex);
		}
		final LinearTimeAllocationProblem result = new LinearTimeAllocationProblem(this.travelTimes, this.betaDur_1_s,
				this.betaTravel_1_s, this.betaLateArr_1_s, this.betaEarlyDpt_1_s, this.betaWait_1_s, this.slack_s);
		result.addTrips(this.trips);
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addTrip(final Trip trip) {
		this.trips.add(trip);
	}

	public void addTrips(final List<Trip> tripList) {
		this.trips.addAll(tripList);
	}

	public RealVector getUpdatedDepartureTimes_s() {

		/*
		 * (1) Identify initial solution.
		 */
		final RealVector initialDepartureTimes_s = new ArrayRealVector(this.trips.size());
		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			initialDepartureTimes_s.setEntry(tripIndex, this.trips.get(tripIndex).departureTime_s);
		}

		/*
		 * (1) Solve linearized problem around the old solution, obtain
		 * extrapolated solution.
		 * 
		 */
		final LinearTimeAllocationProblem linearProblem = this.newLinearProblem(initialDepartureTimes_s);
		final RealVector initialGradient_1_s = linearProblem.get__dScore_dDptTimes__1_s();
		final RealVector extrapolatedDepartureTimes_s = new ArrayRealVector(
				(new SimplexSolver()).optimize(new LinearObjectiveFunction(initialGradient_1_s, 0.0),
						linearProblem.getConstraints(), GoalType.MAXIMIZE).getPoint());
		final RealVector extrapolatedGradient_1_s = this.newLinearProblem(extrapolatedDepartureTimes_s)
				.get__dScore_dDptTimes__1_s();

		/*
		 * (2) Line search based on linearly interpolated gradient.
		 * 
		 * grad(eta) = eta * <extrapolGrad, extrapolSol> + * (1-eta) *
		 * <initialGrad, initialSol> = 0
		 * 
		 * eta * (<extrapolGrad, extrapolSol> - <initialGrad, initialSol>) = -
		 * <initialGrad, initialSol>
		 * 
		 * eta = - <initialGrad, initialSol> / (<extrapolGrad, extrapolSol> -
		 * <initialGrad, initialSol>);
		 * 
		 */
		final double initialSlope = initialDepartureTimes_s.dotProduct(initialGradient_1_s);
		final double extrapolatedSlope = extrapolatedDepartureTimes_s.dotProduct(extrapolatedGradient_1_s);
		final double eta;
		if (Math.abs(initialSlope - extrapolatedSlope) < 1e-8) {
			eta = 0;
		} else {
			eta = Math.max(0, Math.min(1, -initialSlope / (extrapolatedSlope - initialSlope)));
		}

		return initialDepartureTimes_s.combine(1.0 - eta, eta, extrapolatedDepartureTimes_s);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
//
//		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600, 24);
//
//		final InterpolatedTravelTimes travelTimes = new InterpolatedTravelTimes(timeDiscretization) {
//			@Override
//			public double get_dTravelTime_dDptTime(Object origin, Object destination, double dptTime_s, Object mode) {
//				return 0;
//			}
//
//			@Override
//			public double getTravelTimeOffset_s(Object origin, Object destination, double dptTime_s, Object mode) {
//				return 1;
//			}
//		};
//
//		final double slack_s = 0.0;
//		final DepartureTimeImprover allocator = new DepartureTimeImprover(travelTimes, 1.0, 0, 0, 0, 0, slack_s);
//		final Trip home2work = new Trip("home", "work", 6.5 * 3600, "car", 1.0, 1.0, Integer.MAX_VALUE, 6 * 3600,
//				Integer.MIN_VALUE, Integer.MAX_VALUE, travelTimes);
//		final Trip work2home = new Trip("work", "home", 17.5 * 3600, "car", 1.0, 1.0, 17 * 3600, Integer.MIN_VALUE,
//				Integer.MIN_VALUE, Integer.MAX_VALUE, travelTimes);
//		allocator.addTrip(home2work);
//		allocator.addTrip(work2home);
//
//		System.out.println(allocator.getUpdatedDepartureTimes_s());
	}
}
