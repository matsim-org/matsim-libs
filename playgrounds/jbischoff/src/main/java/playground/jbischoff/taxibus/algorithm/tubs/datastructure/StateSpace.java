package playground.jbischoff.taxibus.algorithm.tubs.datastructure;

public interface StateSpace {

	//returns expected number of future confirmations
	double getValue(double time, double slack);
	double getCurrentLastArrivalTime(double now);
}
