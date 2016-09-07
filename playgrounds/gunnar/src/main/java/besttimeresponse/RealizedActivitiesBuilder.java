package besttimeresponse;

import static besttimeresponse.PlannedActivity.MINACTDUR_S;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 */
public class RealizedActivitiesBuilder<L, M> {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	private final TripTravelTimes<L, M> travelTimes;

	private final boolean repairTimeStructure;

	private final boolean interpolateTravelTimes;

	private final List<PlannedActivity<L, M>> plannedActs = new ArrayList<>();

	private final List<Double> dptTimes_s = new ArrayList<>();

	private List<RealizedActivity<L, M>> result = null;

	// -------------------- CONSTRUCTION --------------------

	public RealizedActivitiesBuilder(final TimeDiscretization timeDiscr, final TripTravelTimes<L, M> travelTimes,
			final boolean repairTimeStructure, final boolean interpolateTravelTimes) {
		this.timeDiscr = timeDiscr;
		this.travelTimes = travelTimes;
		this.repairTimeStructure = repairTimeStructure;
		this.interpolateTravelTimes = interpolateTravelTimes;
	}

	// -------------------- BUILDING PROCESS --------------------

	public void addActivity(final PlannedActivity<L, M> act, final double dptTime_s) {
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
		double lastArrTime_s = -MINACTDUR_S;

		for (int q = 0; q < _N; q++) {

			if (this.dptTimes_s.get(q) < lastArrTime_s + MINACTDUR_S) {
				if (this.repairTimeStructure) {
					this.dptTimes_s.set(q, lastArrTime_s + MINACTDUR_S);
				} else {
					throw new RuntimeException("Departure time is " + this.dptTimes_s.get(q)
							+ "s but the last arrival time is " + lastArrTime_s + "s. This is not feasible.");
				}
			}

			final PlannedActivity<L, M> prevAct = this.plannedActs.get(q);
			final PlannedActivity<L, M> nextAct = this.plannedActs.get((q + 1 < _N) ? (q + 1) : 0);

			final int dptBin = this.timeDiscr.getBin(this.dptTimes_s.get(q));
			final double ttAtDptTimeBinStart_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
					this.timeDiscr.getBinStartTime_s(dptBin), prevAct.departureMode);

			double dTT_dDptTime;
			final double ttOffset_s;
			if (this.interpolateTravelTimes) {
				final double ttAtDptTimeBinEnd_s = this.travelTimes.getTravelTime_s(prevAct.location, nextAct.location,
						this.timeDiscr.getBinEndTime_s(dptBin), prevAct.departureMode);
				dTT_dDptTime = (ttAtDptTimeBinEnd_s - ttAtDptTimeBinStart_s) / this.timeDiscr.getBinSize_s();
				if ((dTT_dDptTime <= -(1.0 - 1e-8)) && this.repairTimeStructure) {
					dTT_dDptTime = -(1.0 - 2e-8);
				}
				ttOffset_s = ttAtDptTimeBinStart_s - dTT_dDptTime * this.timeDiscr.getBinStartTime_s(dptBin);
			} else {
				dTT_dDptTime = 0.0;
				ttOffset_s = ttAtDptTimeBinStart_s;
			}

			lastArrTime_s = (1.0 + dTT_dDptTime) * this.dptTimes_s.get(q) + ttOffset_s;
			final int arrBin = this.timeDiscr.getBin(lastArrTime_s);

			final double minDptTime_s = max(this.timeDiscr.getBinStartTime_s(dptBin),
					(this.timeDiscr.getBinStartTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			final double maxDptTime_s = min(this.timeDiscr.getBinEndTime_s(dptBin),
					(this.timeDiscr.getBinEndTime_s(arrBin) - ttOffset_s) / (1.0 + dTT_dDptTime));
			tripTimes.add(new TripTime(dTT_dDptTime, ttOffset_s, minDptTime_s, maxDptTime_s));
		}

		this.result = new ArrayList<>(_N);
		double arrTime_s = tripTimes.get(_N - 1).getArrTime_s(this.dptTimes_s.get(_N - 1)); // home
		for (int q = 0; q < _N; q++) {
			this.result.add(new RealizedActivity<L, M>(this.plannedActs.get(q), tripTimes.get(q), arrTime_s,
					this.dptTimes_s.get(q)));
			arrTime_s = tripTimes.get(q).getArrTime_s(this.dptTimes_s.get(q));
		}
		this.result = Collections.unmodifiableList(this.result);
	}

	public List<RealizedActivity<L, M>> getResult() {
		return this.result;
	}
}
