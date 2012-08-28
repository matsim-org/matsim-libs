package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;


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

	
	/**
	 * As bin size is 15min, at the borders of two parking activities.
	 * 
	 * attention: this algorithm is not able to remove blur errors, if max capacity is not yet reached.
	 */
	public void removeBlurErrors(int maxCapacity){
		for (int i = 0; i < numberOfBins; i++) {
			if (occupancy[i]>maxCapacity){
				if (i!=0 && i!=numberOfBins-1){
					if (occupancy[i-1]!=occupancy[i] && occupancy[i]!=occupancy[i+1]){
						occupancy[i]=maxCapacity;
					} else {
						DebugLib.emptyFunctionForSettingBreakPoint();
					}
				} else if(i==0) {
					if (occupancy[numberOfBins-1]!=occupancy[i] && occupancy[i]!=occupancy[i+1]){
						occupancy[i]=maxCapacity;
					} else {
						DebugLib.emptyFunctionForSettingBreakPoint();
					}
				} else if(i==numberOfBins-1){
					if (occupancy[i-1]!=occupancy[i] && occupancy[i]!=occupancy[0]){
						occupancy[i]=maxCapacity;
					} else {
						DebugLib.emptyFunctionForSettingBreakPoint();
					}
				} else {
					DebugLib.stopSystemAndReportInconsistency();
				}
			}
		}
	}
	
	public boolean isMaximumCapacityConstraintViolated(int maxCapacity){
		for (int i = 0; i < numberOfBins; i++) {
			if (occupancy[i]>maxCapacity){
				return true;
			}
		}
		return false;
	}
	
	public boolean isMaximumCapacityConstraintViolatedForFirstTime(int maxCapacity){
		for (int i = 0; i < numberOfBins; i++) {
			if (occupancy[i]>maxCapacity+1){
				return false;
			}
			if (occupancy[i]==maxCapacity+1){
				return true;
			}
		}
		return false;
	}
	
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

	// TODO: look at this again: we count the first bin, but not the last...
	public void inrementParkingOccupancy(double startTime, double endTime) {
		int startBinIndex = getBinIndex(startTime);
		int endBinIndex = getBinIndex(endTime);

		if (startBinIndex > endBinIndex) {

			for (int i = startBinIndex; i < numberOfBins; i++) {
				occupancy[i]++;
			}

			for (int i = 0; i < endBinIndex; i++) {
				occupancy[i]++;
			}

		} else {
			for (int i = startBinIndex; i < endBinIndex; i++) {
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
