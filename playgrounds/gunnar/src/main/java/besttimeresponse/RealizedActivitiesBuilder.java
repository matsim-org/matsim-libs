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

	private final LinkedList<TripTimes> trips = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public RealizedActivitiesBuilder(final TimeDiscretization timeDiscr, final TravelTimes travelTimes) {
		this.timeDiscr = timeDiscr;
		this.travelTimes = travelTimes;
	}

	// -------------------- BUILDING PROCESS (INPUT) --------------------

	public boolean expectingTrip() {
		// if both lists are empty, an activity is expected
		return (this.trips.size() < this.plannedActs.size());
	}

	public boolean expectingAct() {
		return !this.expectingTrip();
	}

	public Double lastArrTime_s() {
		if (this.trips.size() == 0) {
			return null;
		} else {
			return this.trips.getLast().getArrTime_s(this.dptTimes_s.getLast());
		}
	}

	public void addTrip(final double dptTime_s) {

		if (!this.expectingTrip()) {
			throw new RuntimeException("No trip expected!");
		}

		final Double lastArrTime_s = this.lastArrTime_s();
		if ((lastArrTime_s != null) && (dptTime_s < lastArrTime_s)) {
			throw new RuntimeException(
					"Departure time (" + dptTime_s + "s) is before last arrival time (" + lastArrTime_s + "s).");
		}

		final int dptBin = this.timeDiscr.getBin(dptTime_s);
		final double ttAtDptTimeBinStart_s = this.travelTimes.getTravelTime_s(this.timeDiscr.getBinStartTime_s(dptBin),
				this.plannedActs.getLast().departureMode);
		final double ttAtDptTimeBinEnd_s = this.travelTimes.getTravelTime_s(this.timeDiscr.getBinEndTime_s(dptBin),
				this.plannedActs.getLast().departureMode);

		final double dTT_dDptTime = (ttAtDptTimeBinEnd_s - ttAtDptTimeBinStart_s) / this.timeDiscr.getBinSize_s();
		if (dTT_dDptTime <= -(1.0 - 1e-8)) {
			throw new RuntimeException("FIFO problem: dTT/dDptTime = " + dTT_dDptTime + " is (almost) below -1.0.");
		}
		final double travelTimeOffset_s = ttAtDptTimeBinStart_s
				- dTT_dDptTime * this.timeDiscr.getBinStartTime_s(dptBin);
		final double arrTime_s = (1.0 + dTT_dDptTime) * dptTime_s + travelTimeOffset_s;
		if (arrTime_s >= Units.S_PER_D) {
			throw new RuntimeException("Arrival time " + arrTime_s + "s is beyond midnight, this is not allowed.");
		}
		final int arrBin = this.timeDiscr.getBin(arrTime_s);

		final double minDptTime_s = max(this.timeDiscr.getBinStartTime_s(dptBin),
				(this.timeDiscr.getBinStartTime_s(arrBin) - travelTimeOffset_s) / (1.0 + dTT_dDptTime));
		final double maxDptTime_s = min(this.timeDiscr.getBinEndTime_s(dptBin),
				(this.timeDiscr.getBinEndTime_s(arrBin) - travelTimeOffset_s) / (1.0 + dTT_dDptTime));

		this.dptTimes_s.add(dptTime_s);
		this.trips.add(new TripTimes(dTT_dDptTime, travelTimeOffset_s, minDptTime_s, maxDptTime_s));
	}

	public void addActivity(final PlannedActivity act) {
		if (!this.expectingAct()) {
			throw new RuntimeException("No activity expected!");
		}
		this.plannedActs.add(act);
	}

	// -------------------- BUILDING PROCESS (OUTPUT) --------------------

	public List<RealizedActivity> getResult() {
		final List<RealizedActivity> result = new ArrayList<>(this.plannedActs.size());
		result.add(new RealizedActivity(this.plannedActs.get(0), this.trips.getFirst(),
				this.trips.getLast().getArrTime_s(this.dptTimes_s.getLast()), this.dptTimes_s.getFirst()));
		for (int q = 1; q < this.plannedActs.size(); q++) {
			result.add(new RealizedActivity(this.plannedActs.get(q), this.trips.get(q),
					this.trips.get(q - 1).getArrTime_s(this.dptTimes_s.get(q - 1)), this.dptTimes_s.get(q)));
		}
		return result;
	}
}
