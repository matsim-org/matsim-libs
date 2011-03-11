package playground.wrashid.parkingSearch.planLevel.occupancy;

import playground.wrashid.lib.GeneralLib;

/**
 * 15 minute bins for the whole day (96 bins) for one single parking.
 * 
 * bin 1: [0min,15min) bin 2: [15min,30min) ...
 * 
 * @author rashid_waraich
 * 
 */
public class ParkingOccupancyBins {

	private int[] occupancy = null;

	public int[] getOccupancy() {
		return occupancy;
	}

	/**
	 * Initialize the data structures.
	 */
	public ParkingOccupancyBins() {
		occupancy = new int[96];

		for (int i = 0; i < 96; i++) {
			occupancy[i] = 0;
		}
	}
	
	public int getPeakOccupanyOfDay(){
		int peakOccupancy=0;
		for (int i = 0; i < 96; i++) {
			if (peakOccupancy<occupancy[i]){
				peakOccupancy=occupancy[i];
			}
		}
		return peakOccupancy;
	}

	public int getOccupancy(double time) {
		int binIndex = getBinIndex(time);

		return occupancy[binIndex];
	}

	public void inrementParkingOccupancy(double startTime, double endTime) {
		int startBinIndex = getBinIndex(startTime);
		int endBinIndex = getBinIndex(endTime);

		if (startBinIndex > endBinIndex) {

			for (int i = startBinIndex; i < 96; i++) {
				occupancy[i]++;
			}

			for (int i = 0; i <= endBinIndex; i++) {
				occupancy[i]++;
			}

		} else {
			for (int i = startBinIndex; i <= endBinIndex; i++) {
				occupancy[i]++;
			}
		}
	}

	/**
	 * return value is in [0,96)
	 * 
	 * @param time
	 * @return
	 */
	public int getBinIndex(double time) {
		return GeneralLib.getTimeBinIndex(time,900.0);
	}

}
