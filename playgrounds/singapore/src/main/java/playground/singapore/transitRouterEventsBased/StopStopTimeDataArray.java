package playground.singapore.transitRouterEventsBased;

public class StopStopTimeDataArray implements StopStopTimeData {

	//Attributes
	private double[] stopStopTimes;
	private int[] numTimes;

	//Constructors
	public StopStopTimeDataArray(int numSlots) {
		stopStopTimes = new double[numSlots];
		numTimes = new int[numSlots];
		resetStopStopTimes();
	}

	//Methods
	@Override
	public int getNumData(int timeSlot) {
		return numTimes[timeSlot<stopStopTimes.length?timeSlot:(stopStopTimes.length-1)];
	}
	@Override
	public double getStopStopTime(int timeSlot) {
		return stopStopTimes[timeSlot<stopStopTimes.length?timeSlot:(stopStopTimes.length-1)];
	}
	@Override
	public synchronized void addStopStopTime(int timeSlot, double stopStopTime) {
		stopStopTimes[timeSlot] = (stopStopTimes[timeSlot]*numTimes[timeSlot]+stopStopTime)/++numTimes[timeSlot];
	}
	@Override
	public void resetStopStopTimes() {
		for(int i=0; i<stopStopTimes.length; i++) {
			stopStopTimes[i] = 0;
			numTimes[i] = 0;
		}
	}

}
