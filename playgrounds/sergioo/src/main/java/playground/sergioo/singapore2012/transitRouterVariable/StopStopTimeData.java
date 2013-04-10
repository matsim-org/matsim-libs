package playground.sergioo.singapore2012.transitRouterVariable;

public interface StopStopTimeData {

	int getNumData(int i);
	double getStopStopTime(int i);
	void addStopStopTime(final int timeSlot, final double stopStopTime);
	void resetStopStopTimes();

}
