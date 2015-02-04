package playground.artemc.transitRouter.stopStopTimes;

import java.io.Serializable;

public interface StopStopTimeData extends Serializable {

	int getNumData(int i);
	double getStopStopTime(int i);
	void addStopStopTime(final int timeSlot, final double stopStopTime);
	void resetStopStopTimes();
	double getStopStopTimeVariance(int timeSlot);

}
