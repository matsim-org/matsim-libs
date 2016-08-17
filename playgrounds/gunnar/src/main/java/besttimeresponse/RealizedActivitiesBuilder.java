package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RealizedActivitiesBuilder {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTimes travelTimes;

	private final LinkedList<PlannedActivity> plannedActs = new LinkedList<>();

	private final LinkedList<Double> dptTimes_s = new LinkedList<>();

	private List<RealizedActivity> result = null;

	// -------------------- CONSTRUCTION --------------------

	public RealizedActivitiesBuilder(final TimeDiscretization timeDiscr, final TravelTimes travelTimes) {
		this.timeDiscr = timeDiscr;
		this.travelTimes = travelTimes;
	}

	// -------------------- BUILDING PROCESS --------------------

	public void addActivity(final PlannedActivity act, final double dptTime_s) {
		if (result != null) {
			throw new RuntimeException("The build process has already been completed. Create a new builder instance.");
		}
		this.plannedActs.add(act);
		this.dptTimes_s.add(dptTime_s);
	}

	public void build() {

		final int _N = this.plannedActs.size();
		if (_N < 2) {
			throw new RuntimeException("Number of activities is " + _N + " but at least two activites are expected.");
		}

		final LinkedList<TripTime> tripTimes = new LinkedList<>();
		double lastArrTime_s = 0.0;
		for (int q = 0; q < _N; q++) {

			if (this.dptTimes_s.get(q) < lastArrTime_s) {
				throw new RuntimeException("Departure time is " + this.dptTimes_s.get(q)
						+ "s but the last arrival time is " + lastArrTime_s + "s. This is not feasible.");
			}

			final PlannedActivity prevAct = this.plannedActs.get(q);
			final PlannedActivity nextAct = this.plannedActs.get(q < _N - 1 ? q + 1 : 0);

			final int dptBin = this.timeDiscr.getBin(this.dptTimes_s.get(q));
			final double ttAtDptTimeBinStart_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
					this.timeDiscr.getBinStartTime_s(dptBin), prevAct.departureMode);
			final double ttAtDptTimeBinEnd_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
					this.timeDiscr.getBinEndTime_s(dptBin), prevAct.departureMode);

			final double dTT_dDptTime = (ttAtDptTimeBinEnd_s - ttAtDptTimeBinStart_s) / this.timeDiscr.getBinSize_s();
			final double ttOffset_s = ttAtDptTimeBinStart_s - dTT_dDptTime * this.timeDiscr.getBinStartTime_s(dptBin);

			lastArrTime_s = (1.0 + dTT_dDptTime) * this.dptTimes_s.get(q) + ttOffset_s;
			if (lastArrTime_s >= Units.S_PER_D) {
				throw new RuntimeException(
						"Arrival time " + lastArrTime_s + "s is beyond midnight, this is not allowed.");
			}
			final int arrBin = this.timeDiscr.getBin(lastArrTime_s);

			final double minDptTime_s = max(this.timeDiscr.getBinStartTime_s(dptBin),
					(this.timeDiscr.getBinStartTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			final double maxDptTime_s = min(this.timeDiscr.getBinEndTime_s(dptBin),
					(this.timeDiscr.getBinEndTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			tripTimes.add(new TripTime(dTT_dDptTime, ttOffset_s, minDptTime_s, maxDptTime_s));
		}

		this.result = new ArrayList<>(_N);
		this.result.add(new RealizedActivity(this.plannedActs.get(0), tripTimes.getFirst(),
				tripTimes.getLast().getArrTime_s(this.dptTimes_s.getLast()), this.dptTimes_s.getFirst()));
		for (int q = 1; q < _N; q++) {
			this.result.add(new RealizedActivity(this.plannedActs.get(q), tripTimes.get(q),
					tripTimes.get(q - 1).getArrTime_s(this.dptTimes_s.get(q - 1)), this.dptTimes_s.get(q)));
		}
	}

	public List<RealizedActivity> getResult() {
		return this.result;
	}
}
