package playground.singapore.transitRouterEventsBased;

public interface StopStopTimeData {

	int getNumData(int i);
	double getStopStopTime(int i);
	void addStopStopTime(final int timeSlot, final double stopStopTime);
	void resetStopStopTimes();

}
