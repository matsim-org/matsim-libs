package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TripTime {

	// -------------------- CONSTANTS --------------------

	final double dTT_dDptTime;

	final double ttOffset_s;

	final double minDptTime_s;

	final double maxDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	TripTime(final double dTT_dDptTime, final double ttOffset_s, final double minDptTime_s, final double maxDptTime_s) {
		if (dTT_dDptTime <= -(1.0 - 1e-8)) {
			throw new RuntimeException("FIFO problem: dTT/dDptTime = " + dTT_dDptTime
					+ " is (almost) below -1.0. This means that a later departure implies an earlier "
					+ "arrival, which may cause the numerical solver problems.");
		}
		if (minDptTime_s > maxDptTime_s) {
			throw new RuntimeException(
					"Infeasible departure time interval [" + minDptTime_s + "s, " + maxDptTime_s + "s].");
		}
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
