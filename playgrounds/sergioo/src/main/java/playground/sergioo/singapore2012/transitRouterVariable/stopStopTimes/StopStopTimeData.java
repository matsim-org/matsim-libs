package playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes;

public interface StopStopTimeData {

	int getNumData(int i);
	double getStopStopTime(int i);
	void addStopStopTime(final int timeSlot, final double stopStopTime);
	void resetStopStopTimes();
	double getStopStopTimeVariance(int timeSlot);

}
