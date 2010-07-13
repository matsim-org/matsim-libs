package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.Id;

/**
 * 15 minute bins for the whole day (96 bins) for one single parking.
 * 
 * bin 1: [0min,15min) bin 2: [15min,30min) ...
 * 
 * @author rashid_waraich
 * 
 */
public class ParkingOccupancyBins {

	private int[] minOccupancy = null;
	private int[] maxOccupancy = null;
	private double[] averageOccupancy = null;
	private int[] numberOfDataPointsForBin=null;

	/**
	 * Initialize the data structures.
	 */
	public ParkingOccupancyBins() {
		minOccupancy = new int[96];
		maxOccupancy = new int[96];
		averageOccupancy = new double[96];
		numberOfDataPointsForBin = new int[96];

		for (int i = 0; i < 96; i++) {
			numberOfDataPointsForBin[i]=0;
			averageOccupancy[i]=0;
			minOccupancy[i] = Integer.MAX_VALUE;
			maxOccupancy[i] = Integer.MIN_VALUE;
		}
	}

	public void setParkingOccupancy(int curOccupancy, double time) {
		int binIndex = getBinIndex(time);

		if (minOccupancy[binIndex] > curOccupancy) {
			minOccupancy[binIndex] = curOccupancy;
		}

		if (maxOccupancy[binIndex] < curOccupancy) {
			maxOccupancy[binIndex] = curOccupancy;
		}
		
		averageOccupancy[binIndex]=(numberOfDataPointsForBin[binIndex]*averageOccupancy[binIndex]+curOccupancy)/(numberOfDataPointsForBin[binIndex]+1);
		numberOfDataPointsForBin[binIndex]++;
	}

	public int getMinOccupancy(double time) {
		return minOccupancy[getBinIndex(time)];
	}

	public int getMaxOccupancy(double time) {
		return maxOccupancy[getBinIndex(time)];
	}
	
	public double getAverageOccupancy(double time) {
		return averageOccupancy[getBinIndex(time)];
	} 
	

	/**
	 * return value is in [0,96)
	 * 
	 * if time is > 60*60*24 [seconds], it will be projected into next day,
	 * e.g. time=60*60*24+1=1
	 * 
	 * @param time
	 * @return
	 */
	public int getBinIndex(double time) {
		double secondsInOneDay = 60 * 60 * 24;

		if (time >= secondsInOneDay) {
			time = ((time / secondsInOneDay) - (Math.floor(time / secondsInOneDay)))*secondsInOneDay;
		}

		return Math.round((float) Math.floor(time / 900.0));
	}

}
