package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearTimeAllocationProblem2 {

	// -------------------- MEMBERS --------------------

	// (linearly interpolated) travel times
	private final InterpolatedTravelTimes travelTimes;

	// (chronological) sequence of trips to be time-optimized
	// private final List<Trip> trips = new ArrayList<>();
	private final RealizedTimeStructure timeStructure;

	// coefficient for duration of an activity
	private final double betaDur_1_s;

	// coefficient for travel duration
	private final double betaTravel_1_s;

	// coefficient for early departure
	private final double betaEarlyDpt_1_s;

	// coefficient for late arrival
	private final double betaLateArr_1_s;

	// coefficient for waiting in front of a closed facility
	private final double betaWait_1_s;

	// slack in time interval bound constraints
	// private final double slack_s;

	// -------------------- CONSTRUCTION --------------------

	public LinearTimeAllocationProblem2(final InterpolatedTravelTimes travelTimes,
			final RealizedTimeStructure timeStructure, final double betaDur_1_s, final double betaTravel_1_s,
			final double betaLateArr_1_s, final double betaEarlyDpt_1_s, final double betaWait_1_s,
			final double slack_s) {
		this.travelTimes = travelTimes;
		this.timeStructure = timeStructure;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
		this.betaWait_1_s = betaWait_1_s;
		// this.slack_s = slack_s;
	}

	// -------------------- HELPERS --------------------

	// private int getStep(final double time_s) {
	// return this.travelTimes.getTimeDiscretization().getBin(time_s);
	// }
	//
	// private int getBinStartTime_s(final int bin) {
	// return this.travelTimes.getTimeDiscretization().getBinStartTime_s(bin);
	// }
	//
	// private int getBinEndTime_s(final int bin) {
	// return this.travelTimes.getTimeDiscretization().getBinEndTime_s(bin);
	// }

	// -------------------- IMPLEMENTATION --------------------

	// public void addTrip(final Trip trip) {
	// this.trips.add(trip);
	// }
	//
	// public void addTrips(final List<Trip> tripList) {
	// this.trips.addAll(tripList);
	// }

	// public LinearTimeAllocationProblem(InterpolatedTravelTimes travelTimes,
	// double betaDur_1_s2, double betaTravel_1_s2,
	// double betaLateArr_1_s2, double betaEarlyDpt_1_s2, double betaWait_1_s2,
	// double slack_s2) {
	// throw new UnsupportedOperationException();
	// }

	public RealVector get__dScore_dDptTimes__1_s() {

		// An attempt to keep variable names consistent with latex writeup.
		final int _N = this.timeStructure.realizedActivities.size();
		
		final double[] a = new double[_N];
		for (int q = 0; q < _N - 1; q++) {
			a[q] = this.travelTimes.getEntry(this.timeStructure.realizedActivities.get(q).plannedActivity,
					this.timeStructure.realizedActivities.get(q + 1).plannedActivity,
					this.timeStructure.realizedActivities.get(q).realizedDepartureTime_s).dTravelTime_dDptTime;
		}

		// Compute individual gradient components.

		final RealVector dSDur_dDptTimes__1_s = new ArrayRealVector(_N);
		final RealVector dSEarlyDpt_dDptTimes__1_s = new ArrayRealVector(_N);
		final RealVector dSTT_dDptTimes__1_s = new ArrayRealVector(_N);
		for (int q = 0; q < this.timeStructure.realizedActivities.size(); q++) {
			final RealizedActivity act = this.timeStructure.realizedActivities.get(q);
			if (!act.getClosedAtDeparture()) {
				dSDur_dDptTimes__1_s.setEntry(q, this.betaDur_1_s * act.getEffectiveTimePressure());
			}
			if (act.getEarlyDeparture()) {
				dSEarlyDpt_dDptTimes__1_s.setEntry(q, -this.betaEarlyDpt_1_s);
			}
			dSTT_dDptTimes__1_s.addToEntry(q, this.betaTravel_1_s * a[q]);
		}

		final RealVector dSNextDur_dDptTimes__1_s = new ArrayRealVector(_N);
		final RealVector dSNextLateArr_dDptTimes__1_s = new ArrayRealVector(_N);
		for (int q = 0; q < this.timeStructure.realizedActivities.size() - 1; q++) {
			final RealizedActivity nextAct = this.timeStructure.realizedActivities.get(q + 1);
			if (!nextAct.getClosedAtArrival()) {
				dSNextDur_dDptTimes__1_s.setEntry(q,
						(1 + a[q]) * this.betaDur_1_s * nextAct.getEffectiveTimePressure());
			}
			if (nextAct.getLateArrival()) {
				dSNextLateArr_dDptTimes__1_s.setEntry(q, this.betaLateArr_1_s * (1.0 + a[q]));
			}
		}
		dSNextDur_dDptTimes__1_s.setEntry(_N - 1, (1 + a[_N - 1]) * this.betaDur_1_s
				* this.timeStructure.realizedActivities.get(0).getEffectiveTimePressure());

		// Combine individual terms into one gradient vector.

		final RealVector dScore_dDptTimes__1_s = dSDur_dDptTimes__1_s.copy();
		dScore_dDptTimes__1_s.add(dSNextDur_dDptTimes__1_s);
		dScore_dDptTimes__1_s.add(dSNextLateArr_dDptTimes__1_s);
		dScore_dDptTimes__1_s.add(dSEarlyDpt_dDptTimes__1_s);
		dScore_dDptTimes__1_s.add(dSTT_dDptTimes__1_s);
		return dScore_dDptTimes__1_s;
	}

	public LinearObjectiveFunction getObjectiveFunction() {
		return new LinearObjectiveFunction(this.get__dScore_dDptTimes__1_s(), 0.0);
	}

	public LinearConstraintSet getConstraints() {
		final List<LinearConstraint> constraints = new ArrayList<>();

		// The first departure must not occur before midnight.
		{
			// final RealVector coeffs = new
			// ArrayRealVector(this.timeStructure.activities.size());
			// coeffs.setEntry(0, 1.0);
			// constraints.add(new LinearConstraint(coeffs, Relationship.GEQ,
			// 0.0));
		}

		// The remaining constraints apply to all trip departure times.
		// for (int tripIndex = 0; tripIndex <
		// this.timeStructure.activities.size(); tripIndex++) {
		// final Trip trip = this.timeStructure.getTrip(tripIndex);
		{
			/*-----------------------------------------------------------------
			 * LOWER BOUND CONSTRAINTS.
			 * 
			 * (1) Lower bound on departure time (to stay within departure time bin):
			 * 
			 * dptTime >= start(dptTimeBin)
			 * 
			 * (2) Lower bound on arrival time (to stay within arrival time bin):
			 * 
			 * arrivalTime >= start(arrivalTimeBin)
			 *    
			 *                dTravelTime
			 *  =>  dptTime + ----------- * dptTime + dptTimeOffset  >=  start(arrivalTimeBin)
			 *                dDptTime   
			 * 
			 *                   start(arrivalTimeBin) - dptTimeOffset
			 * <=>  dptTime  >=  -------------------------------------
			 *                        1 + dTravelTime / dDptTime 
			 *             
			 * Combined into one lower bound by taking the maximum of the lower 
			 * bounds (1) and (2). An additional slack may be allowed for.
			 *                               
			 * TODO This assumes that one is dividing through a strictly 
			 * positive number.                               
			 *-----------------------------------------------------------------
			 */
			{
				// final RealVector coeffs = new
				// ArrayRealVector(this.timeStructure.activities.size());
				// coeffs.setEntry(tripIndex, 1.0);
				// final double lowerBound = Math.max(dptTimeStepStart_s,
				// (arrTimeStepStart_s - travelTimeOffset_s) / (1.0 +
				// dTravelTime_dDptTime));
				// constraints.add(new LinearConstraint(coeffs,
				// Relationship.GEQ, lowerBound - this.slack_s));
			}

			/*-----------------------------------------------------------------
			 * UPPER BOUND CONSTRAINTS.
			 * 
			 * (1) Upper bound on departure time (to stay within departure time bin):
			 * 
			 * dptTime <= end(dptTimeBin)
			 * 
			 * (2) Upper bound on arrival time (to stay within arrival time bin):
			 * 
			 * arrivalTime <= end(arrivalTimeBin)
			 *    
			 *                dTravelTime
			 *  =>  dptTime + ----------- * dptTime + dptTimeOffset  <=  end(arrivalTimeBin)
			 *                dDptTime   
			 * 
			 *                   end(arrivalTimeBin) - dptTimeOffset
			 * <=>  dptTime  <=  -----------------------------------
			 *                       1 + dTravelTime / dDptTime
			 *             
			 * Combined into one upper bound by taking the minimum of the upper 
			 * bounds (1) and (2). An additional slack may be allowed for.                             
			 *                   
			 * TODO This assumes that one is dividing through a strictly 
			 * positive number.                               
			 *-----------------------------------------------------------------
			 */
			{
				// final RealVector coeffs = new
				// ArrayRealVector(this.timeStructure.activities.size());
				// coeffs.setEntry(tripIndex, 1.0);
				// final double upperBound = Math.min(dptTimeStepEnd_s,
				// (arrTimeStepEnd_s - travelTimeOffset_s) / (1.0 +
				// dTravelTime_dDptTime));
				// constraints.add(new LinearConstraint(coeffs,
				// Relationship.LEQ, upperBound + this.slack_s));
			}

			/*-----------------------------------------------------------------
			 * TODO Not sure if this is of any use.
			 * 
			 * DURATION OF NEXT ACTIVITY MUST BE NON-NEGATIVE.
			 * 
			 * nextArrTime <= nextDptTime
			 * 
			 * =>  dptTime + travelTime(dptTime) <= nextDptTime
			 * 
			 * =>  (1 + dTravelTime / dDptTime) * dptTime + travelTimeOffset <= nextDptTime
			 * 
			 * <=>  (1 + dTravelTime / dDptTime) * dptTime - nextDptTime <= -travelTimeOffset
			 * 
			 * TODO Making the activity duration strictly positive would help
			 * to deal with negative-infinity problems in the logarithmic 
			 * activity duration scoring.
			 * 
			 * FIXME 24hr wrap-around is not accounted for.
			 *-----------------------------------------------------------------
			 */
			// {
			// final int nextTripIndex;
			// if (tripIndex == this.trips.size() - 1) {
			// nextTripIndex = 0;
			// } else {
			// nextTripIndex = tripIndex + 1;
			// }
			// final RealVector coeffs = new
			// ArrayRealVector(this.timeStructure.activities.size());
			// coeffs.setEntry(tripIndex, 1.0 + dTravelTime_dDptTime);
			// coeffs.setEntry(nextTripIndex, -1.0);
			// constraints.add(new LinearConstraint(coeffs, Relationship.LEQ,
			// -travelTimeOffset_s));
			// }
		}

		return new LinearConstraintSet(constraints);
	}

	public void addTrips(List<Trip> trips) {
		throw new UnsupportedOperationException();
	}
}
