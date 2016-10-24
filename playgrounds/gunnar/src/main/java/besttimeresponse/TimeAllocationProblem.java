package besttimeresponse;

import static besttimeresponse.PlannedActivity.MINACTDUR_S;
import static java.lang.Math.log;
import static java.lang.Math.max;
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
class TimeAllocationProblem {

	// -------------------- MEMBERS --------------------

	// chronological list of realized activities
	private final List<RealizedActivity<?, ?>> realizedActivities;

	// overlap between travel time bins
	private final double slack_s = 1.0;

	// -------------------- CONSTRUCTION --------------------

	<L, M> TimeAllocationProblem(final List<RealizedActivity<L, M>> realizedActivities) {
		this.realizedActivities = new ArrayList<RealizedActivity<?, ?>>(realizedActivities);
	}

	// -------------------- IMPLEMENTATION --------------------

	double[] getInitialSolution() {
		double[] result = new double[this.realizedActivities.size()];
		for (int q = 0; q < this.realizedActivities.size(); q++) {
			result[q] = this.realizedActivities.get(q).realizedDptTime_s;
		}
		return result;
	}

	double getTimeScoreAtInitialSolution() {
		final int _N = this.realizedActivities.size();
		double result = 0.0;
		for (int q = 0; q < _N; q++) {
			final RealizedActivity<?, ?> act = this.realizedActivities.get(q);
			result += act.plannedActivity.betaDur_1_s * act.plannedActivity.desiredDur_s
					* log(max(MINACTDUR_S, act.effectiveDuration_s())
							/ max(MINACTDUR_S, act.plannedActivity.zeroUtilityDur_s));
			if (act.isLateArrival()) {
				result += act.plannedActivity.betaLateArr_1_s
						* (act.realizedArrTime_s - act.plannedActivity.latestArrTime_s);
			}
			if (act.isEarlyDeparture()) {
				result += act.plannedActivity.betaEarlyDpt_1_s
						* (act.plannedActivity.earliestDptTime_s - act.realizedDptTime_s);
			}
			result += act.plannedActivity.betaTravel_1_s
					* (this.realizedActivities.get((q + 1) < _N ? (q + 1) : 0).realizedArrTime_s
							- act.realizedDptTime_s);
		}
		return result;
	}

	RealVector get__dScore_dDptTimes__1_s() {

		// For consistency with formula write-up.

		final int _N = this.realizedActivities.size();

		// Compose gradient vector.

		final RealVector dScore_dDptTimes__1_s = new ArrayRealVector(_N);

		for (int q = 0; q < _N; q++) {

			final RealizedActivity<?, ?> act = this.realizedActivities.get(q);
			final RealizedActivity<?, ?> nextAct = this.realizedActivities.get(((q + 1) < _N) ? (q + 1) : 0);
			final double a = act.nextTripTravelTime.dTT_dDptTime;

			// travel time
			dScore_dDptTimes__1_s.addToEntry(q, act.plannedActivity.betaTravel_1_s * a);

			// current activity duration
			if (!act.isClosedAtDeparture()) {
				dScore_dDptTimes__1_s.addToEntry(q, act.plannedActivity.betaDur_1_s * act.plannedActivity.desiredDur_s
						/ max(MINACTDUR_S, act.effectiveDuration_s()));
			}

			// next activity duration
			if (!nextAct.isClosedAtArrival()) {
				dScore_dDptTimes__1_s.addToEntry(q, (-1.0) * (1.0 + a) * nextAct.plannedActivity.betaDur_1_s
						* nextAct.plannedActivity.desiredDur_s / max(MINACTDUR_S, nextAct.effectiveDuration_s()));
			}

			// early departure from current activity
			if (act.isEarlyDeparture()) {
				dScore_dDptTimes__1_s.addToEntry(q, -act.plannedActivity.betaEarlyDpt_1_s);
			}

			// late arrival at next activity
			if (nextAct.isLateArrival()) {
				dScore_dDptTimes__1_s.addToEntry(q, nextAct.plannedActivity.betaLateArr_1_s * (1.0 + a));
			}
		}

		return dScore_dDptTimes__1_s;
	}

	LinearObjectiveFunction getObjectiveFunction() {
		final RealVector coeffs = this.get__dScore_dDptTimes__1_s();
		final double offset = this.getTimeScoreAtInitialSolution()
				- coeffs.dotProduct(new ArrayRealVector(this.getInitialSolution()));
		return new LinearObjectiveFunction(coeffs, offset);
	}

	LinearConstraintSet getConstraints() {

		// For notational consistency with formula write-up.

		final int _N = this.realizedActivities.size();
		final double[] a = new double[_N];
		final double[] b = new double[_N];
		for (int q = 0; q < _N; q++) {
			final TripTime tripTravelTime = this.realizedActivities.get(q).nextTripTravelTime;
			a[q] = tripTravelTime.dTT_dDptTime;
			b[q] = tripTravelTime.ttOffset_s;
		}

		final List<LinearConstraint> constraints = new ArrayList<>();

		// The first departure must not occur before midnight.
		{
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(0, 1.0);
			constraints.add(new LinearConstraint(coeffs, GEQ, 0.0));
		}

		// Departure from an activity must not happen before arrival.
		for (int q = 0; q < _N - 1; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, -(1.0 + a[q]));
			coeffs.setEntry(q + 1, 1.0);
			constraints.add(new LinearConstraint(coeffs, GEQ, MINACTDUR_S + b[q]));
		}

		// The last arrival must occur before midnight.
		// {
		// final RealVector coeffs = new ArrayRealVector(_N);
		// coeffs.setEntry(_N - 1, (1.0 + a[_N - 1]));
		// constraints.add(new LinearConstraint(coeffs, LEQ, Units.S_PER_D -
		// b[_N - 1]));
		// }

		// lower bound on departure time due to time discretization
		for (int q = 0; q < _N; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, 1.0);
			constraints.add(new LinearConstraint(coeffs, GEQ,
					this.realizedActivities.get(q).nextTripTravelTime.minDptTime_s - this.slack_s));
		}

		// upper bound on departure time due to time discretization
		for (int q = 0; q < _N; q++) {
			final RealVector coeffs = new ArrayRealVector(_N);
			coeffs.setEntry(q, 1.0);
			constraints.add(new LinearConstraint(coeffs, LEQ,
					this.realizedActivities.get(q).nextTripTravelTime.maxDptTime_s + this.slack_s));
		}

		return new LinearConstraintSet(constraints);
	}
}
