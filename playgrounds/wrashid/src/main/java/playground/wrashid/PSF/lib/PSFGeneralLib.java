package playground.wrashid.PSF.lib;

import playground.wrashid.lib.GeneralLib;

public class PSFGeneralLib {

	// write hubs to file
	public static void writeEnergyUsageStatisticsData(String fileName, double[][] matrix) {
		int numberOfHubs=matrix[0].length;
		String headerLine = "";

		for (int i = 1; i < numberOfHubs; i++) {
			headerLine += "hub-" + i + "\t";
		}

		headerLine += "hub-" + numberOfHubs + "\t";

		GeneralLib.writeMatrix(matrix, fileName, headerLine);
	}
	
	 
}
