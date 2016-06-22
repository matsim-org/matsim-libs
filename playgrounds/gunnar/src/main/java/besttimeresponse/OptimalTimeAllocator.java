package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
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
public class OptimalTimeAllocator {

	// -------------------- MEMBERS --------------------

	private final InterpolatedTravelTimes travelTimes;

	private final double betaDur_1_s;

	private final double betaTravel_1_s;

	private final double betaLateArr_1_s;

	private final double betaEarlyDpt_1_s;

	private final double betaWait_1_s;

	private final List<Trip> trips = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public OptimalTimeAllocator(final InterpolatedTravelTimes travelTimes, final double betaDur_1_s,
			final double betaTravel_1_s, final double betaLateArr_1_s, final double betaEarlyDpt_1_s,
			final double betaWait_1_s) {
		this.travelTimes = travelTimes;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
		this.betaWait_1_s = betaWait_1_s;
	}

	// -------------------- HELPERS --------------------

	private LinearTimeAllocationProblem newLinearProblem(final RealVector departureTimes_s) {
		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			this.trips.get(tripIndex).departureTime_s = departureTimes_s.getEntry(tripIndex);
		}
		final LinearTimeAllocationProblem result = new LinearTimeAllocationProblem(this.travelTimes, this.betaDur_1_s,
				this.betaTravel_1_s, this.betaLateArr_1_s, this.betaEarlyDpt_1_s, this.betaWait_1_s);
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

	public void run() {
		final RealVector initialDepartureTimes_s = new ArrayRealVector(this.trips.size());
		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			initialDepartureTimes_s.setEntry(tripIndex, this.trips.get(tripIndex).departureTime_s);
		}
		this.run(initialDepartureTimes_s);
	}

	public void run(final RealVector initialDepartureTimes_s) {

		RealVector currentDepartureTimes_s = initialDepartureTimes_s;
		RealVector currentGradient_1_s = null;

		RealVector intermediateDepartureTimes_s = null;
		RealVector intermediateGradient_1_s = null;

		System.out.println(currentDepartureTimes_s);

		boolean solutionHasChanged = true;
		do {

			/*
			 * (1) Solve linearized problem around the old solution.
			 * 
			 */
			{
				final LinearTimeAllocationProblem linearProblem = this.newLinearProblem(currentDepartureTimes_s);
				currentGradient_1_s = linearProblem.get__dScore_dDptTimes__1_s();
				final LinearObjectiveFunction intermediateObjectiveFunction = new LinearObjectiveFunction(
						currentGradient_1_s, 0.0);
				final LinearConstraintSet intermediateConstraints = linearProblem.getConstraints();
				intermediateDepartureTimes_s = new ArrayRealVector((new SimplexSolver())
						.optimize(intermediateObjectiveFunction, intermediateConstraints, GoalType.MAXIMIZE)
						.getPoint());
				intermediateGradient_1_s = this.newLinearProblem(intermediateDepartureTimes_s)
						.get__dScore_dDptTimes__1_s();
			}

			/*
			 * (2) Line search based on linearly interpolated gradient.
			 * 
			 * grad(eta) = eta * <newGrad, newSol> + * (1-eta) * <oldGrad,
			 * oldSol> = 0
			 * 
			 * eta * (<newGrad, newSol> - <oldGrad, oldSol>) = - <oldGrad,
			 * oldSol>
			 * 
			 * eta = - <oldGrad, oldSol> / (<newGrad, newSol> - <oldGrad,
			 * oldSol>);
			 * 
			 */
			final double oldSlope = currentDepartureTimes_s.dotProduct(currentGradient_1_s);
			final double newSlope = intermediateDepartureTimes_s.dotProduct(intermediateGradient_1_s);
			final double eta;
			if (Math.abs(oldSlope - newSlope) < 1e-8) {
				eta = 0;
			} else {
				eta = Math.max(0, Math.min(1, -oldSlope / (newSlope - oldSlope)));
			}
			currentDepartureTimes_s.combineToSelf(1.0 - eta, eta, intermediateDepartureTimes_s);

			/*
			 * (3) Compute termination criterion.
			 */
			solutionHasChanged = (eta > 1e-8);

			System.out.println(currentDepartureTimes_s);

		} while (solutionHasChanged);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600, 24);

		final InterpolatedTravelTimes travelTimes = new InterpolatedTravelTimes(timeDiscretization) {
			@Override
			public double get_dTravelTime_dDptTime(Object origin, Object destination, double dptTime_s, Object mode) {
				return 0;
			}

			@Override
			public double getTravelTimeOffset_s(Object origin, Object destination, double dptTime_s, Object mode) {
				return 1;
			}
		};

		final OptimalTimeAllocator allocator = new OptimalTimeAllocator(travelTimes, 1.0, 0, 0, 0, 0);
		final Trip home2work = new Trip("home", "work", 6.5 * 3600, "car", 1.0, 1.0, Integer.MAX_VALUE, 6 * 3600,
				Integer.MIN_VALUE, Integer.MAX_VALUE, travelTimes);
		final Trip work2home = new Trip("work", "home", 17.5 * 3600, "car", 1.0, 1.0, 17 * 3600, Integer.MIN_VALUE,
				Integer.MIN_VALUE, Integer.MAX_VALUE, travelTimes);
		allocator.addTrip(home2work);
		allocator.addTrip(work2home);
		allocator.run();
	}
}
