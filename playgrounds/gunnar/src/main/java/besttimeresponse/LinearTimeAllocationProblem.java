package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.Relationship;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearTimeAllocationProblem {

	// -------------------- MEMBERS --------------------

	private final InterpolatedTravelTimes interpolatedTravelTimes;

	private final double betaDur_1_s;

	private final double betaTravel_1_s;

	private final double betaEarlyDpt_1_s;

	private final double betaLateArr_1_s;

	private final double betaWait_1_s;

	private final List<Trip> trips = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LinearTimeAllocationProblem(final InterpolatedTravelTimes interpolatedTravelTimes, final double betaDur_1_s,
			final double betaTravel_1_s, final double betaLateArr_1_s, final double betaEarlyDpt_1_s,
			final double betaWait_1_s) {
		this.interpolatedTravelTimes = interpolatedTravelTimes;
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
		this.betaWait_1_s = betaWait_1_s;
	}

	// -------------------- HELPERS --------------------

	private int getBin(final double time_s) {
		return this.interpolatedTravelTimes.getTimeDiscretization().getBin(time_s);
	}

	private int getBinStartTime_s(final int bin) {
		return this.interpolatedTravelTimes.getTimeDiscretization().getBinStartTime_s(bin);
	}

	private int getBinEndTime_s(final int bin) {
		return this.interpolatedTravelTimes.getTimeDiscretization().getBinEndTime_s(bin);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addTrip(final Object origin, final Object destination, final int departureTime_s, final Object mode,
			final double originTimePressure, final double destinationTimePressure, final int originClosingTime_s,
			final int earliestDepartureTime_s, final int destinationOpeningTime_s, final int latestArrivalTime_s) {

		final int departureTimeStep = this.getBin(departureTime_s);
		final double arrivalTime_s = departureTime_s
				+ this.interpolatedTravelTimes.getTravelTime_s(origin, destination, departureTime_s, mode);
		final int arrivalTimeStep = this.getBin(arrivalTime_s);

		final int originClosingTimeStep = this.getBin(originClosingTime_s);
		final boolean originClosesBeforeDeparture = (originClosingTimeStep <= departureTimeStep);

		final int earliestDepartureTimeStep = this.getBin(earliestDepartureTime_s);
		final boolean earlyDepartureFromOrigin = (departureTimeStep < earliestDepartureTimeStep);

		final int destinationOpeningTimeStep = this.getBin(destinationOpeningTime_s);
		final boolean destinationOpensAfterArrival = (destinationOpeningTimeStep <= arrivalTimeStep);

		final int latestArrivalTimeStep = this.getBin(latestArrivalTime_s);
		final boolean lateArrivalToDestination = (latestArrivalTimeStep <= arrivalTimeStep);

		this.trips.add(new Trip(origin, destination, departureTime_s, mode, originTimePressure, destinationTimePressure,
				originClosesBeforeDeparture, earlyDepartureFromOrigin, destinationOpensAfterArrival,
				lateArrivalToDestination));
	}

	public RealVector get__dScore_dDptTimes__1_s() {

		final RealVector dScore_dDptTimes__1_s = new ArrayRealVector(this.trips.size());

		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {

			final Trip trip = this.trips.get(tripIndex);

			final double dTravelTime_dDptTime = this.interpolatedTravelTimes.get_dTravelTime_dDptTime(trip);

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON PREVIOUS ACTIVITY DURATION SCORE.
			 * 
			 * If the previous closing time binds, i.e if 
			 * dptTime > closingTime, then there is no effect. If the previous 
			 * closing time does not bind, then
			 * 
			 * S_act = beta_dur * dur_desired * log(dur_realized)
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
			 * If there is no early departure, i.e if dptTime > earliest.dp,
			 * then there is no effect. If there is an early departure, then
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
			 * EFFECT OF DEPARTURE TIME ON FOLLOWING ACTIVITY DURATION SCORE.
			 * 
			 * If the following opening time binds, i.e if 
			 * dptTime + travelTime(dptTime) < openingTime, then there is no 
			 * effect. If the following opening time does not bind, then
			 * 
			 * dS_act                               dDur_realized
			 * -------- = beta_dur * timePressure * -------------
			 * dDptTime                             dDptTime
			 * 
			 * where
			 * 
			 * dDur_realized   dDur_realized   dArrivalTime     dArrivalTime
			 * ------------- = ------------- * ------------ = - ------------.
			 * dDptTime        dArrivalTime    dDptTime         dDptTime
			 * 
			 * From
			 * 
			 * arrivalTime = dptTime + travelTime(dptTime)
			 * 
			 * one has
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
			 * EFFECT OF DEPARTURE TIME ON WAIT-FOR-FOLLOWING ACTIVITY SCORE.
			 * 
			 * If the following opening time does not bind, i.e if 
			 * dptTime + travelTime(dptTime) > openingTime, then there is no 
			 * effect. If the following opening time does bind, then
			 * 
			 * S_wait = beta_wait * (openingTime - arrivalTime)
			 * 
			 * dS_wait                  dArrivalTime
			 * -------- = - beta_wait * ------------
			 * dDptTime                 dDptTime
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
				dScore_dDptTimes__1_s.addToEntry(tripIndex, -this.betaWait_1_s * (1.0 + dTravelTime_dDptTime));
			}

			/*-----------------------------------------------------------------
			 * EFFECT OF DEPARTURE TIME ON LATE ARRIVAL SCORE.
			 * 	
			 * If there is no late arrival, i.e. arrivalTime < latest.arr, 
			 * then there is no effect. If there is a late arrival, then
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
			 * EFFECT ON TRAVEL TIME DURATION.
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

	public LinearConstraintSet getLinearConstraints() {
		final List<LinearConstraint> constraints = new ArrayList<>();

		/*-----------------------------------------------------------------
		 * THE FIRST DEPARTURE MUST NOT OCCUR BEFORE MIDNIGHT.
		 * ----------------------------------------------------------------
		 */
		{
			final RealVector coeffs = new ArrayRealVector(this.trips.size());
			coeffs.setEntry(0, 1.0);
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0.0));
		}

		for (int tripIndex = 0; tripIndex < this.trips.size(); tripIndex++) {

			final Trip trip = this.trips.get(tripIndex);
			final int nextTripIndex;
			if (tripIndex == this.trips.size() - 1) {
				nextTripIndex = 0;
			} else {
				nextTripIndex = tripIndex + 1;
			}

			final int dptTimeBin = this.getBin(trip.departureTime_s);
			final int dptTimeStepStart_s = this.getBinStartTime_s(dptTimeBin);
			final int dptTimeStepEnd_s = this.getBinEndTime_s(dptTimeBin);

			final double arrivalTime_s = trip.departureTime_s + this.interpolatedTravelTimes.getTravelTime_s(trip);
			final int arrTimeStep = this.getBin(arrivalTime_s);
			final int arrTimeStepStart_s = this.getBinStartTime_s(arrTimeStep);
			final int arrTimeStepEnd_s = this.getBinEndTime_s(arrTimeStep);

			final double dTravelTime_dDptTime = this.interpolatedTravelTimes.get_dTravelTime_dDptTime(trip);
			final double travelTimeOffset_s = this.interpolatedTravelTimes.getTravelTimeOffset_s(trip);

			/*-----------------------------------------------------------------
			 * LOWER BOUND CONSTRAINTS.
			 * 
			 * (1) Lower bound on departure time (to stay within departure time bin):
			 * 
			 * dptTime >= minValue(dptTimeBin)
			 * 
			 * (2) Lower bound on arrival time (to stay within arrival time bin):
			 * 
			 * arrivalTime >= minValue(arrivalTimeBin)
			 *    
			 *                dTravelTime
			 *  =>  dptTime + ----------- * dptTime + dptTimeOffset  >=  minValue(arrivalTimeBin)
			 *                dDptTime   
			 * 
			 *                   minValue(arrivalTimeBin) - dptTimeOffset
			 * <=>  dptTime  >=  ----------------------------------------
			 *                              dTravelTime 
			 *                          1 + ----------- 
			 *                              dDptTime    
			 *             
			 * Combined into one lower bound by taking the maximum of the lower bounds (1) and (2).                             
			 *                               
			 *-----------------------------------------------------------------
			 */
			{
				final RealVector lowerBoundCoeffs = new ArrayRealVector(this.trips.size());
				lowerBoundCoeffs.setEntry(tripIndex, 1.0);
				final double lowerBound = Math.max(dptTimeStepStart_s,
						(arrTimeStepStart_s - travelTimeOffset_s) / (1.0 + dTravelTime_dDptTime));
				constraints.add(new LinearConstraint(lowerBoundCoeffs, Relationship.GEQ, lowerBound));
			}

			/*-----------------------------------------------------------------
			 * UPPER BOUND CONSTRAINTS.
			 * 
			 * (1) Upper bound on departure time (to stay within departure time bin):
			 * 
			 * dptTime <= maxValue(dptTimeBin)
			 * 
			 * (2) Upper bound on arrival time (to stay within arrival time bin):
			 * 
			 * arrivalTime <= maxValue(arrivalTimeBin)
			 *    
			 *                dTravelTime
			 *  =>  dptTime + ----------- * dptTime + dptTimeOffset  <=  maxValue(arrivalTimeBin)
			 *                dDptTime   
			 * 
			 *                   maxValue(arrivalTimeBin) - dptTimeOffset
			 * <=>  dptTime  <=  ----------------------------------------
			 *                              dTravelTime 
			 *                          1 + ----------- 
			 *                              dDptTime    
			 *             
			 * Combined into one upper bound by taking the minimum of the upper bounds (1) and (2).                             
			 *                               
			 *-----------------------------------------------------------------
			 */
			{
				final RealVector upperBoundCoeffs = new ArrayRealVector(this.trips.size());
				upperBoundCoeffs.setEntry(tripIndex, 1.0);
				final double upperBound = Math.min(dptTimeStepEnd_s,
						(arrTimeStepEnd_s - travelTimeOffset_s) / (1.0 + dTravelTime_dDptTime));
				constraints.add(new LinearConstraint(upperBoundCoeffs, Relationship.LEQ, upperBound));
			}

			/*-----------------------------------------------------------------
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
			 *-----------------------------------------------------------------
			 */
			{
				final RealVector coeffs = new ArrayRealVector(this.trips.size());
				coeffs.setEntry(tripIndex, 1.0 + dTravelTime_dDptTime);
				coeffs.setEntry(nextTripIndex, -1.0);
				constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, -travelTimeOffset_s));
			}
		}

		return new LinearConstraintSet(constraints);
	}
}
