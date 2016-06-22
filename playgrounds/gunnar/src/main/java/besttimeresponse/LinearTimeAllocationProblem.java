package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearTimeAllocationProblem {

	// -------------------- MEMBERS --------------------

	private final InterpolatedTravelTimes travelTimes;

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
	private final double slack_s;

	// (chronological) sequence of trips to be time-optimized
	private final List<Trip> trips = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LinearTimeAllocationProblem(final InterpolatedTravelTimes interpolatedTravelTimes, final double betaDur_1_s,
			final double betaTravel_1_s, final double betaLateArr_1_s, final double betaEarlyDpt_1_s,
			final double betaWait_1_s, final double slack_s) {
		this.travelTimes = interpolatedTravelTimes;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
		this.betaWait_1_s = betaWait_1_s;
		this.slack_s = slack_s;
	}

	// -------------------- HELPERS --------------------

	private int getStep(final double time_s) {
		return this.travelTimes.getTimeDiscretization().getBin(time_s);
	}

	private int getBinStartTime_s(final int bin) {
		return this.travelTimes.getTimeDiscretization().getBinStartTime_s(bin);
	}

	private int getBinEndTime_s(final int bin) {
		return this.travelTimes.getTimeDiscretization().getBinEndTime_s(bin);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addTrip(final Trip trip) {
		this.trips.add(trip);
	}

	public void addTrips(final List<Trip> tripList) {
		this.trips.addAll(tripList);
	}

	public RealVector get__dScore_dDptTimes__1_s() {

		final RealVector dScore_dDptTimes__1_s = new ArrayRealVector(this.trips.size());

		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			final Trip trip = this.trips.get(tripIndex);

			final double dTravelTime_dDptTime = this.travelTimes.get_dTravelTime_dDptTime(trip);

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON ORIGIN ACTIVITY DURATION SCORE.
			 *
			 * S_act = beta_dur * dur_desired * log(dur_realized)
			 *
			 * If the origin activity closes before departure, then there is 
			 * no effect. Otherwise,
			 * 
			 * dS_act                dur_desired    dDur_realized
			 * -------- = beta_dur * ------------ * -------------
			 * dDptTime              dur_realized   dDptTime
			 * 
			 * where
			 * 
			 *                dur_desired
			 * timePressure = ------------
			 *                dur_realized
			 * 
			 * and
			 * 
			 * dDur_realized    
			 * ------------- = 1.
			 * dDptTime         
			 * 
			 * ----------------------------------------------------------------
			 */
			if (!trip.originClosesBeforeDeparture) {
				dScore_dDptTimes__1_s.addToEntry(tripIndex, this.betaDur_1_s * trip.originTimePressure);
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON EARLY DEPARTURE SCORE.
			 * 	
			 * If there is no early departure, then there is no effect. 
			 * Otherwise,
			 * 
			 * S_early.dpt = beta_early.dp * (earliest.dp - dptTime)
			 * 
			 * dS_early              
			 * -------- = - beta_early.dp.
			 * dDptTime                
			 * 
			 * ----------------------------------------------------------------
			 */
			if (trip.earlyDepartureFromOrigin) {
				dScore_dDptTimes__1_s.addToEntry(tripIndex, -this.betaEarlyDpt_1_s);
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON DESTINATION ACTIVITY DURATION SCORE.
			 * 
			 * If the destination activity opens after arrival, then there is 
			 * no effect. Otherwise,
			 * 
			 * dS_act                               dDur_realized
			 * -------- = beta_dur * timePressure * -------------
			 * dDptTime                             dDptTime
			 * 
			 * where
			 * 
			 * dDur_realized   dDur_realized   dArrivalTime   - dArrivalTime
			 * ------------- = ------------- * ------------ = --------------.
			 * dDptTime        dArrivalTime    dDptTime       dDptTime
			 * 
			 * From
			 * 
			 * arrivalTime = dptTime + travelTime(dptTime),
			 * 
			 * one obtains
			 *  
			 * dArrivalTime       dTravelTime
			 * ------------ = 1 + -----------.
			 * dDptTime           dDptTime
			 * 
			 * ----------------------------------------------------------------
			 */
			if (!trip.destinationOpensAfterArrival) {
				dScore_dDptTimes__1_s.addToEntry(tripIndex,
						this.betaDur_1_s * trip.destinationTimePressure * -(1.0 + dTravelTime_dDptTime));
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON WAIT-FOR-DESTINATION-ACTIVITY SCORE.
			 * 
			 * This only has an effect if the destination activity opens after
			 * arrival. Then,
			 * 
			 * S_wait = beta_wait * (openingTime - arrivalTime)
			 * 
			 * dS_wait                - dArrivalTime
			 * -------- = beta_wait * ------------
			 * dDptTime               dDptTime
			 * 
			 * where, again,
			 *  
			 * dArrivalTime       dTravelTime
			 * ------------ = 1 + -----------.
			 * dDptTime           dDptTime
			 * 
			 * ----------------------------------------------------------------
			 */
			if (trip.destinationOpensAfterArrival) {
				dScore_dDptTimes__1_s.addToEntry(tripIndex, this.betaWait_1_s * -(1.0 + dTravelTime_dDptTime));
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON LATE ARRIVAL AT DESTINATION SCORE.
			 * 	
			 * This only has an effect if there is a late arrival. Then,
			 * 
			 * S_late.ar = beta_late.ar * (arrivalTime - latest.ar)
			 * 
			 * dS_late.ar                  dArrivalTime
			 * ---------- = beta_late.ar * ------------
			 * dDptTime                    dDptTime
			 * 
			 * where, again,
			 * 
			 * dArrivalTime       dTravelTime
			 * ------------ = 1 + -----------.
			 * dDptTime           dDptTime
			 * 
			 * ----------------------------------------------------------------
			 */
			if (trip.lateArrivalToDestination) {
				dScore_dDptTimes__1_s.addToEntry(tripIndex, this.betaLateArr_1_s * (1.0 + dTravelTime_dDptTime));
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON TRAVEL TIME DURATION.
			 * 	
			 * S_travel = beta_travel * travelTime(dptTime)
			 * 
			 * dS_travel                 dTravelTime
			 * --------- = beta_travel * -----------.
			 * dDptTime                  dDptTime
			 * 
			 * ----------------------------------------------------------------
			 */
			dScore_dDptTimes__1_s.addToEntry(tripIndex, this.betaTravel_1_s * dTravelTime_dDptTime);
		}

		return dScore_dDptTimes__1_s;
	}

	public LinearObjectiveFunction getObjectiveFunction() {
		return new LinearObjectiveFunction(this.get__dScore_dDptTimes__1_s(), 0.0);
	}

	public LinearConstraintSet getConstraints() {
		final List<LinearConstraint> constraints = new ArrayList<>();

		// The first departure must not occur before midnight.
		{
			final RealVector coeffs = new ArrayRealVector(this.trips.size());
			coeffs.setEntry(0, 1.0);
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0.0));
		}

		// The remaining constraints apply to all trip departure times.
		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {
			final Trip trip = this.trips.get(tripIndex);

			final int dptTimeStep = this.getStep(trip.departureTime_s);
			final double dptTimeStepStart_s = this.getBinStartTime_s(dptTimeStep);
			final double dptTimeStepEnd_s = this.getBinEndTime_s(dptTimeStep);

			final double arrivalTime_s = this.travelTimes.getArrivalTime_s(trip);
			final int arrTimeStep = this.getStep(arrivalTime_s);
			final double arrTimeStepStart_s = this.getBinStartTime_s(arrTimeStep);
			final double arrTimeStepEnd_s = this.getBinEndTime_s(arrTimeStep);

			final double dTravelTime_dDptTime = this.travelTimes.get_dTravelTime_dDptTime(trip);
			final double travelTimeOffset_s = this.travelTimes.getTravelTimeOffset_s(trip);

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
				final RealVector coeffs = new ArrayRealVector(this.trips.size());
				coeffs.setEntry(tripIndex, 1.0);
				final double lowerBound = Math.max(dptTimeStepStart_s,
						(arrTimeStepStart_s - travelTimeOffset_s) / (1.0 + dTravelTime_dDptTime));
				constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, lowerBound - this.slack_s));
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
				final RealVector coeffs = new ArrayRealVector(this.trips.size());
				coeffs.setEntry(tripIndex, 1.0);
				final double upperBound = Math.min(dptTimeStepEnd_s,
						(arrTimeStepEnd_s - travelTimeOffset_s) / (1.0 + dTravelTime_dDptTime));
				constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, upperBound + this.slack_s));
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
			// final RealVector coeffs = new ArrayRealVector(this.trips.size());
			// coeffs.setEntry(tripIndex, 1.0 + dTravelTime_dDptTime);
			// coeffs.setEntry(nextTripIndex, -1.0);
			// constraints.add(new LinearConstraint(coeffs, Relationship.LEQ,
			// -travelTimeOffset_s));
			// }
		}

		return new LinearConstraintSet(constraints);
	}
}
