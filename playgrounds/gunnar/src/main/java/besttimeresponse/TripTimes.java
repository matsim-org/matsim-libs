package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TripTimes {

	// -------------------- CONSTANTS --------------------

	final double dTT_dDptTime;

	final double ttOffset_s;

	final double minDptTime_s;

	final double maxDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	TripTimes(final double dTT_dDptTime, final double ttOffset_s, final double minDptTime_s,
			final double maxDptTime_s) {
		this.dTT_dDptTime = dTT_dDptTime;
		this.ttOffset_s = ttOffset_s;
		this.minDptTime_s = minDptTime_s;
		this.maxDptTime_s = maxDptTime_s;
	}

	// -------------------- GETTERS --------------------

	double getTT_s(final double dptTime_s) {
		return this.dTT_dDptTime * dptTime_s + this.ttOffset_s;
	}

	double getArrTime_s(final double dptTime_s) {
		return dptTime_s + this.getTT_s(dptTime_s);
	}
}
