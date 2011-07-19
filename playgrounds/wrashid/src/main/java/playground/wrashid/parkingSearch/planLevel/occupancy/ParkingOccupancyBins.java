package playground.wrashid.parkingSearch.planLevel.occupancy;

import playground.wrashid.lib.DebugLib;
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

	int numberOfBins=96;
	private int[] occupancy = null;

	public int[] getOccupancy() {
		return occupancy;
	}

	/**
	 * Initialize the data structures.
	 */
	public ParkingOccupancyBins() {
		init();
	}

	private void init() {
		occupancy = new int[numberOfBins];

		for (int i = 0; i < numberOfBins; i++) {
			occupancy[i] = 0;
		}
	}
	
	public ParkingOccupancyBins(int numberOfBins) {
		this.numberOfBins=numberOfBins;
		init();
		DebugLib.stopSystemAndReportInconsistency("probably won't work, as times are projected within 24 hours.");
	}
	
	public int getPeakOccupanyOfDay(){
		int peakOccupancy=0;
		for (int i = 0; i < numberOfBins; i++) {
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

			for (int i = startBinIndex; i < numberOfBins; i++) {
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
		return GeneralLib.getTimeBinIndex(time,3600.0*24.0/numberOfBins);
	}

}
