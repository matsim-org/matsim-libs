package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

public class StopStopTimeDataArray implements StopStopTimeData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Attributes
	private double[] stopStopTimeMeans;
	private double[] stopStopTimeSquares;
	private int[] numTimes;

	//Constructors
	public StopStopTimeDataArray(int numSlots) {
		stopStopTimeSquares = new double[numSlots];
		stopStopTimeMeans = new double[numSlots];
		numTimes = new int[numSlots];
		resetStopStopTimes();
	}

	//Methods
	@Override
	public int getNumData(int timeSlot) {
		return numTimes[timeSlot<stopStopTimeMeans.length?timeSlot:(stopStopTimeMeans.length-1)];
	}
	@Override
	public double getStopStopTime(int timeSlot) {
		return stopStopTimeMeans[timeSlot<stopStopTimeMeans.length?timeSlot:(stopStopTimeMeans.length-1)];
	}
	@Override
	public double getStopStopTimeVariance(int timeSlot) {
		int index = timeSlot<stopStopTimeMeans.length?timeSlot:(stopStopTimeMeans.length-1);
		return stopStopTimeSquares[index]-Math.pow(stopStopTimeMeans[index], 2);
	}
	@Override
	public synchronized void addStopStopTime(int timeSlot, double stopStopTime) {
		stopStopTimeSquares[timeSlot] = (stopStopTimeSquares[timeSlot]*numTimes[timeSlot]+Math.pow(stopStopTime, 2))/(numTimes[timeSlot]+1);
		stopStopTimeMeans[timeSlot] = (stopStopTimeMeans[timeSlot]*numTimes[timeSlot]+stopStopTime)/++numTimes[timeSlot];		
	}
	@Override
	public void resetStopStopTimes() {
		for(int i=0; i<stopStopTimeMeans.length; i++) {
			stopStopTimeMeans[i] = 0;
			numTimes[i] = 0;
		}
	}

}
