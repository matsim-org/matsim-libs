package besttimeresponse;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class InterpolatedTripTravelTime {

	// -------------------- CONSTANTS --------------------

	public final double dTravelTime_dDptTime;

	public final double travelTimeOffset_s;

	public final double minDptTime_s;

	public final double maxDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	public InterpolatedTripTravelTime(final double timeBinSize_s, final int dptTimeBin, int arrTimeBin,
			final double ttAtDptTimeBinStart_s, final double ttAtDptTimeBinEnd_s) {

		this.dTravelTime_dDptTime = (ttAtDptTimeBinEnd_s - ttAtDptTimeBinStart_s) / timeBinSize_s;
		if (this.dTravelTime_dDptTime <= -(1.0 - 1e-8)) {
			throw new RuntimeException(
					"FIFO problem: dTT/dDptTime = " + this.dTravelTime_dDptTime + " (approx) <= -1.0.");
		}
		this.travelTimeOffset_s = ttAtDptTimeBinStart_s - this.dTravelTime_dDptTime * (timeBinSize_s * dptTimeBin);

		this.minDptTime_s = max(timeBinSize_s * dptTimeBin,
				(timeBinSize_s * arrTimeBin - this.travelTimeOffset_s) / (1.0 + this.dTravelTime_dDptTime));
		this.maxDptTime_s = min(timeBinSize_s * (dptTimeBin + 1),
				(timeBinSize_s * (arrTimeBin + 1) - this.travelTimeOffset_s) / (1.0 + this.dTravelTime_dDptTime));
	}

	// -------------------- GETTERS --------------------

	public double getTravelTime_s(final double dptTime_s) {
		return this.dTravelTime_dDptTime * dptTime_s + this.travelTimeOffset_s;
	}
}
