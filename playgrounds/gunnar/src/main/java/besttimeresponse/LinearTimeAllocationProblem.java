package besttimeresponse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearTimeAllocationProblem {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final List<Departure> departures = new ArrayList<>();

	private int lastDepartureTimeBin = 0;

	// -------------------- CONSTRUCTION --------------------

	public LinearTimeAllocationProblem(final TimeDiscretization timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	public void addDepartureFromActivity(final double departureTime_s, final double ttAtBinStart_s,
			final double ttAtBinEnd_s, final double desiredPrevDur_s) {
		final int dptTimeBin = this.timeDiscretization.getBin(departureTime_s);
		if (this.lastDepartureTimeBin > dptTimeBin) {
			throw new RuntimeException("Cannot add a departure in time bin " + dptTimeBin
					+ " because this is before the last departure time bin " + this.lastDepartureTimeBin + ".");
		}
		final double a = (ttAtBinEnd_s - ttAtBinStart_s) / this.timeDiscretization.getBinSize_s();
		if (a < -1.0) {
			throw new RuntimeException("FIFO violation; a = " + a + ", which is below -1.");
		}
		final double b = (this.timeDiscretization.getBinEndTime_s(dptTimeBin) * ttAtBinStart_s
				- this.timeDiscretization.getBinStartTime_s(dptTimeBin) * ttAtBinEnd_s)
				/ this.timeDiscretization.getBinSize_s();
		this.departures.add(new Departure(departureTime_s, desiredPrevDur_s, a, b));
	}

	// -------------------- IMPLEMENTATION --------------------

	public void linearize() {

		final RealVector dS_dDptTime_utils_s = new ArrayRealVector(this.departures.size());

		for (int dptTimeIndex = 0; dptTimeIndex < this.departures.size(); dptTimeIndex++) {

			// Effect on duration of previous activity.
			{
				final double dDur_dDptTime = 0.0;
				// TODO CONTINUE HERE
			}
			
			
			// Effect on early departure from previous activity.

			// Effect on duration of following activity.

			// Effect on late arrival to following activity.

			// Effect on travel time duration.

		}

	}

	// -------------------- INNER TYPES --------------------

	private class Departure {

		private final double dptTime_s;

		private final double desiredPrevDur_s;

		private final double a;

		private final double b;

		private Departure(final double dptTime_s, final double desiredPrevDur_s, final double a, final double b) {
			this.dptTime_s = dptTime_s;
			this.desiredPrevDur_s = desiredPrevDur_s;
			this.a = a;
			this.b = b;
		}
	}

}
