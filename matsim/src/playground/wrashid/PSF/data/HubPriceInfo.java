package playground.wrashid.PSF.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

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
		// this.numberOfHubs = numberOfHubs;
		hubPrice = new double[numberOfHubs][numberOfTimeBins];
		
		this.numberOfHubs=numberOfHubs;
		
		try {

			FileReader fr = new FileReader(fileName);

			BufferedReader br = new BufferedReader(fr);
			String line;
			StringTokenizer tokenizer;
			String token;
			line = br.readLine();
			int rowId = 0;
			while (line != null) {
				tokenizer = new StringTokenizer(line);

				for (int i = 0; i < numberOfHubs; i++) {
					token = tokenizer.nextToken();
					double parsedNumber = Double.parseDouble(token);
					hubPrice[i][rowId] = parsedNumber;
				}

				if (tokenizer.hasMoreTokens()) {
					// if there are more columns than expected, throw an
					// exception

					throw new RuntimeException("the number of hubs is wrong");
				}

				line = br.readLine();
				rowId++;
			}

			if (rowId != 96) {
				throw new RuntimeException("the number of rows is wrong");
			}

		} catch (RuntimeException e) {
			// just forward the runtime exception
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Error reading the hub link mapping file");
		}

	}

	/**
	 * time: time of day in seconds hub
	 * 
	 * @param time
	 * @param hubNumber
	 */
	public double getPrice(double time, int hubNumber) {
		return hubPrice[hubNumber][(int) Math.floor(time / (binInterval))];
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
		numberOfHubs=1;
		
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

}
