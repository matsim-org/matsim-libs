package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collections;
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

	private final TripTravelTimes travelTimes;

	private final boolean repairTimeStructure;

	private final List<PlannedActivity> plannedActs = new ArrayList<>();

	private final List<Double> dptTimes_s = new ArrayList<>();

	private List<RealizedActivity> result = null;

	// -------------------- CONSTRUCTION --------------------

	public RealizedActivitiesBuilder(final TimeDiscretization timeDiscr, final TripTravelTimes travelTimes,
			final boolean repairTimeStructure) {
		this.timeDiscr = timeDiscr;
		this.travelTimes = travelTimes;
		this.repairTimeStructure = repairTimeStructure;
	}

	// -------------------- BUILDING PROCESS --------------------

	public void addActivity(final PlannedActivity act, final double dptTime_s) {
		if (result != null) {
			throw new RuntimeException("The build process has already been completed. Create a new builder instance.");
		}
		this.plannedActs.add(act);
		this.dptTimes_s.add(dptTime_s);
	}

	private double withinDayTime_s(double time_s) {
		while (time_s > Units.S_PER_D) {
			time_s -= Units.S_PER_D;
		}
		return time_s;
	}

	public void build() {

		final int _N = this.plannedActs.size();
		if (_N < 2) {
			throw new RuntimeException("Number of activities is " + _N + " but at least two activites are expected.");
		}

		final LinkedList<TripTime> tripTimes = new LinkedList<>();
		double lastArrTime_s = -PlannedActivity.minActDur_s; // at 23:59:59

		for (int q = 0; q < _N; q++) {

			if (this.dptTimes_s.get(q) < lastArrTime_s + PlannedActivity.minActDur_s) {
				if (this.repairTimeStructure) {
					this.dptTimes_s.set(q, lastArrTime_s + PlannedActivity.minActDur_s);
				} else {
					throw new RuntimeException("Departure time is " + this.dptTimes_s.get(q)
							+ "s but the last arrival time is " + lastArrTime_s + "s. This is not feasible.");
				}
			}

			final PlannedActivity prevAct = this.plannedActs.get(q);
			final PlannedActivity nextAct = this.plannedActs.get(q < _N - 1 ? q + 1 : 0);

			final int dptBin = this.timeDiscr.getBin(this.dptTimes_s.get(q));
			final double ttAtDptTimeBinStart_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
					this.timeDiscr.getBinStartTime_s(dptBin), prevAct.departureMode);
			final double ttAtDptTimeBinEnd_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
					this.timeDiscr.getBinEndTime_s(dptBin), prevAct.departureMode);

			double dTT_dDptTime = (ttAtDptTimeBinEnd_s - ttAtDptTimeBinStart_s) / this.timeDiscr.getBinSize_s();
			if (dTT_dDptTime <= -(1.0 - 1e-8)) {
				if (this.repairTimeStructure) {
					dTT_dDptTime = -(1.0 - 1e-8);
				} else {
					throw new RuntimeException(
							"FIFO problem: dTT/dDptTime = " + dTT_dDptTime + " is (almost) below -1.0.");
				}
			}
			final double ttOffset_s = ttAtDptTimeBinStart_s - dTT_dDptTime * this.timeDiscr.getBinStartTime_s(dptBin);

			lastArrTime_s = (1.0 + dTT_dDptTime) * this.dptTimes_s.get(q) + ttOffset_s;
			final int arrBin = this.timeDiscr.getBin(lastArrTime_s);

			final double minDptTime_s = max(this.timeDiscr.getBinStartTime_s(dptBin),
					(this.timeDiscr.getBinStartTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			final double maxDptTime_s = min(this.timeDiscr.getBinEndTime_s(dptBin),
					(this.timeDiscr.getBinEndTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			tripTimes.add(new TripTime(dTT_dDptTime, ttOffset_s, this.withinDayTime_s(minDptTime_s),
					this.withinDayTime_s(maxDptTime_s)));
		}

		this.result = new ArrayList<>(_N);
		for (int q = 0; q < _N; q++) {
			final int prevQ = (q - 1 >= 0 ? q - 1 : _N - 1);
			this.result.add(new RealizedActivity(this.plannedActs.get(q), tripTimes.get(q),
					this.withinDayTime_s(tripTimes.get(prevQ).getArrTime_s(this.dptTimes_s.get(prevQ))),
					this.withinDayTime_s(this.dptTimes_s.get(q))));
		}
		this.result = Collections.unmodifiableList(this.result);
	}

	public List<RealizedActivity> getResult() {
		return this.result;
	}
}
