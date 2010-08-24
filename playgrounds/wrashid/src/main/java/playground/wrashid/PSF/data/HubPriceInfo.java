package playground.wrashid.PSF.data;

import org.matsim.core.utils.charts.XYLineChart;

import playground.wrashid.lib.GeneralLib;

/*
 * - The file is split into 15 minute bins, this means we have 96 entries in the file.
 * - numbering of hubs starts with zero
 * - first value is for 0 to 15.
 */
public class HubPriceInfo {

	// first index: , second index: price (96 entries, for the 15 min bins)
	// bins: [0,15), [15,30),...
	double hubPrice[][];
	private static final int numberOfTimeBins = 96;
	// 24 hours has 96 bins
	private static final double binInterval = 900;
	private int numberOfHubs;

	// (in seconds = 15min)

	public HubPriceInfo(String fileName, int numberOfHubs) {

		this.numberOfHubs = numberOfHubs;
		
		//System.out.println(fileName);
		
		hubPrice = GeneralLib.readMatrix(numberOfTimeBins,numberOfHubs,false,fileName);
		// need to invert the matrix, because hubPrice expects the hub number as the first dimension and
		// the time as the second dimension.
		hubPrice = GeneralLib.invertMatrix(hubPrice);
	}
	
	public double[][] getPriceMatrix(){
		return GeneralLib.invertMatrix(hubPrice);
	}

	/**
	 * time: time of day in seconds hub
	 * 
	 * @param time
	 * @param hubNumber
	 */
	public double getPrice(double time, int hubNumber) {
		// doing modulo 96, just in case the day had more than 24 hours
		return hubPrice[hubNumber][(int) Math.floor(time / (binInterval)) % 96];
	}

	// if only one hub
	public double getPrice(double time) {
		return getPrice(time, 0);
	}

	/**
	 * time in seconds, assumption: only one hub
	 * [peakHourStatTime,peakHourEndTime) the method expects multiples of 900
	 * seconds as input for the times
	 * 
	 * @param peakHourStartTime
	 * @param peakHourEndTime
	 */
	public HubPriceInfo(double peakHourStartTime, double peakHourEndTime, double offPeakRate, double peakRate) {
		numberOfHubs = 1;

		hubPrice = new double[1][numberOfTimeBins];

		for (int i = 0; i < numberOfTimeBins; i++) {
			if ((i * binInterval >= peakHourStartTime && i * binInterval < peakHourEndTime)
					|| ((i * binInterval >= peakHourStartTime || i * binInterval < peakHourEndTime) && peakHourEndTime < peakHourStartTime)) {
				hubPrice[0][i] = peakRate;
			} else {
				hubPrice[0][i] = offPeakRate;
			}
		}
	}
	
	public void writePriceGraph(String fileName){
		XYLineChart chart = new XYLineChart("Hub Energy Prices", "Time of Day [s]", "Price [CHF]");

		double[] time = new double[numberOfTimeBins];

		for (int i = 0; i < numberOfTimeBins; i++) {
			time[i] = i * 900;
		}

		for (int i = 0; i < numberOfHubs; i++) {
			double[] priceInfo = new double[numberOfTimeBins];
			for (int j = 0; j < numberOfTimeBins; j++) {
				// convert from Joule to kWh
				priceInfo[j] = hubPrice[i][j];
			}
			chart.addSeries("hub-" + i, time, priceInfo);
		}

		// chart.addMatsimLogo();
		chart.saveAsPng(fileName, 800, 600);
	}

	/*
	 * print to sdt output the hub, price info as read from the file
	 */
	public void print() {
		for (int i = 0; i < numberOfTimeBins; i++) {
			for (int j = 0; j < numberOfHubs; j++) {
				System.out.print(hubPrice[j][i]);
				System.out.print("\t");
			}
			System.out.println();
		}
	}

	/**
	 * Although we have just one price for the whole area, we want to make
	 * several columns to pretend, we would have several hubs.
	 * 
	 * @param numberOfHubs
	 */
	public void printFakeHubs(int numberOfHubs) {
		// copy the hub columns
		for (int i = 0; i < numberOfTimeBins; i++) {
			for (int j = 0; j < numberOfHubs; j++) {
				System.out.print(hubPrice[0][i]);
				System.out.print("\t");
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		// create hubs
		// attention: if there is too much output, you won't be able to copy it from the console directly.
		// therefore redirect that output to a file and copy it from there!
		//HubPriceInfo hubPriceInfo = new HubPriceInfo(18000, 75600, 9, 18);
		//hubPriceInfo.printFakeHubs(820);
		
		// read hubs
		new HubPriceInfo("A:/data/matsim/input/runRW1003/hubPriceInfo.txt",820);
	}

}
