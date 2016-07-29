package playground.jbischoff.taxibus.algorithm.tubs.datastructure;

public interface StateSpace {

	//returns expected number of future confirmations
	double getValue(double time, double slack);
	double getCurrentLastArrivalTime(double now);
	void addExperiencedTimeSlack(double time, double slack, int confirmations);
	void incBookingCounter();
	boolean acceptableStartTime(double now);
}
