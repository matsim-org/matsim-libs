package besttimeresponse;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.math3.optim.linear.Relationship.GEQ;
import static org.apache.commons.math3.optim.linear.Relationship.LEQ;

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
public class LinearTimeAllocationProblem {

	// -------------------- MEMBERS --------------------

	// chronological list of realized activities
	private final List<RealizedActivity> realizedActivities;

	// coefficient for duration of an activity
	private final double betaDur_1_s;

	// coefficient for travel duration
	private final double betaTravel_1_s;

	// coefficient for early departure
	private final double betaEarlyDpt_1_s;

	// coefficient for late arrival
	private final double betaLateArr_1_s;

	// -------------------- CONSTRUCTION --------------------

	public LinearTimeAllocationProblem(final List<RealizedActivity> realizedActivities, final double betaDur_1_s,
			final double betaTravel_1_s, final double betaLateArr_1_s, final double betaEarlyDpt_1_s) {
		this.realizedActivities = unmodifiableList(realizedActivities);
		this.betaDur_1_s = betaDur_1_s;
		this.betaTravel_1_s = betaTravel_1_s;
		this.betaLateArr_1_s = betaLateArr_1_s;
		this.betaEarlyDpt_1_s = betaEarlyDpt_1_s;
	}

	// -------------------- IMPLEMENTATION --------------------

	public RealVector get__dScore_dDptTimes__1_s() {

		// For consistency with formula write-up.

		final int _N = this.realizedActivities.size();

		// Compose gradient vector.

		final RealVector dScore_dDptTimes__1_s = new ArrayRealVector(_N);

		for (int q = 0; q < _N; q++) {

			final RealizedActivity act = this.realizedActivities.get(q);
			final RealizedActivity nextAct = this.realizedActivities.get((q < _N - 1) ? (q + 1) : 0);
			final double a = act.nextTripTravelTime.dTravelTime_dDptTime;

			// travel time
			dScore_dDptTimes__1_s.addToEntry(q, this.betaTravel_1_s * a);

			// current activity duration
			if (!act.isClosedAtDeparture()) {
				dScore_dDptTimes__1_s.addToEntry(q, this.betaDur_1_s * act.getEffectiveTimePressure());
			}

			// next activity duration
			if (!nextAct.isClosedAtArrival()) {
				dScore_dDptTimes__1_s.addToEntry(q,
						(-1.0) * (1.0 + a) * this.betaDur_1_s * nextAct.getEffectiveTimePressure());
			}

			// early departure from current activity
			if (act.isEarlyDeparture()) {
				dScore_dDptTimes__1_s.addToEntry(q, -this.betaEarlyDpt_1_s);
			}

			// late arrival at next activity
			if (nextAct.isLateArrival()) {
				dScore_dDptTimes__1_s.addToEntry(q, this.betaLateArr_1_s * (1.0 + a));
			}
		}

		return dScore_dDptTimes__1_s;
	}

	public LinearObjectiveFunction getLinearObjectiveFunction() {
		return new LinearObjectiveFunction(this.get__dScore_dDptTimes__1_s(), 0.0);
	}

	public LinearConstraintSet getConstraints() {

		// For consistency with formula write-up.
		
		final int _N = this.realizedActivities.size();
		final double[] a = new double[_N];
		final double[] b = new double[_N];
		for (int q = 0; q < _N; q++) {
			final InterpolatedTripTravelTime tripTravelTime = this.realizedActivities.get(q).nextTripTravelTime;
			a[q] = tripTravelTime.dTravelTime_dDptTime;
			b[q] = tripTravelTime.travelTimeOffset_s;
		}

		final List<LinearConstraint> constraints = new ArrayList<>();

		// The first departure must not occur before midnight.
		{
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(0, 1.0);
			constraints.add(new LinearConstraint(coeffs, GEQ, 0.0));
		}

		// Departure from an activity must not happen before arrival.
		for (int q = 0; q < _N; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, 1.0 + a[q]);
			coeffs.setEntry((q < _N - 1) ? (q + 1) : 0, -1.0);
			constraints.add(new LinearConstraint(coeffs, LEQ, -b[q]));
		}

		// lower bound on departure time due to time discretization
		for (int q = 0; q < _N; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, 1.0);
			constraints.add(
					new LinearConstraint(coeffs, GEQ, this.realizedActivities.get(q).nextTripTravelTime.minDptTime_s));
		}

		// upper bound on departure time due to time discretization
		for (int q = 0; q < _N; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, 1.0);
			constraints.add(
					new LinearConstraint(coeffs, LEQ, this.realizedActivities.get(q).nextTripTravelTime.maxDptTime_s));
		}

		return new LinearConstraintSet(constraints);
	}
}
