package playground.wrashid.parkingChoice.trb2011.counts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class SingleDayGarageParkingsCount {

	public static void main(String[] args) {
		String baseFolder = "H:/data/experiments/TRBAug2011/parkings/counts/";
		StringMatrix countsMatrix = GeneralLib.readStringMatrix(baseFolder + "parkingGarageCountsCityZH27-April-2011.txt", "\t");

		// generateOccupancyPlotsForAllParkings(countsMatrix, baseFolder +
		// "pngs/");

		// ==============================

		// generateParkingOccupancyPlot(countsMatrix,
		// "Parkhaus Urania / Uraniastrasse 3", baseFolder +
		// "ParkHausUraniaOccupancy.png");

		// ===============================

		HashMap<String, Double[]> occupancyOfAllSelectedParkings = getOccupancyOfAllSelectedParkings(countsMatrix);
		double[] sumOfOccupancyCountsOfSelectedParkings = new double[96];		
		
		
		for (String parkingName:occupancyOfAllSelectedParkings.keySet()){
			Double[] occupancyBins = occupancyOfAllSelectedParkings.get(parkingName);
			
			for (int i=0;i<96;i++){
				try {
					sumOfOccupancyCountsOfSelectedParkings[i]+=occupancyBins[i];
				} catch (Exception e) {
					System.out.println();
				}
			}
		}
	}

	public static HashMap<String, Double[]> getOccupancyOfAllSelectedParkings(StringMatrix countsMatrix) {		
		HashMap<String, String> mappingOfParkingNameToParkingId = getMappingOfParkingNameToParkingId();

		LinkedList<String> parkingNames = getParkingNames(countsMatrix);
		
		HashMap<String, Double> parkingCapacities = getParkingCapacities();

		HashMap<String, Double[]> parkingOccupanciesSelectedParkings=new HashMap<String, Double[]>();
		
		for (String parkingName : parkingNames) {
			Double[] freeParkings = getNumberOfFreeParkings(countsMatrix, parkingName);
			if (!containsNullEntries(freeParkings)) {
				if (mappingOfParkingNameToParkingId.containsKey(parkingName)) {
					Double[] occupiedParkings = getNumberOfOccupiedParkings(freeParkings, parkingCapacities.get(parkingName));
					
					parkingOccupanciesSelectedParkings.put(parkingName, occupiedParkings);
					
				}
			}
		}
		
		return parkingOccupanciesSelectedParkings;
	}

	public static void generateOccupancyPlotsForAllParkings(StringMatrix countsMatrix, String outputFolder) {
		LinkedList<String> parkingNames = getParkingNames(countsMatrix);

		HashMap<String, Double> parkingCapacities = getParkingCapacities();

		reportIfParkingCapacityNotFound(parkingNames, parkingCapacities);

		for (String parkingName : parkingNames) {
			Double parkingCapacity = parkingCapacities.get(parkingName);
			generateParkingOccupancyPlot(countsMatrix, parkingName, outputFolder + createFileName(parkingName), parkingCapacity);
		}
	}

	private static void reportIfParkingCapacityNotFound(LinkedList<String> parkingNames, HashMap<String, Double> parkingCapacities) {
		for (String parkingName : parkingNames) {
			if (!parkingCapacities.containsKey(parkingName)) {
				DebugLib.stopSystemAndReportInconsistency("no parking capacity found for:" + parkingName);
			}
		}
	}

	private static LinkedList<String> getParkingNames(StringMatrix countsMatrix) {
		LinkedList<String> parkingNames = new LinkedList<String>();

		for (int i = 0; i < countsMatrix.getNumberOfRows(); i++) {
			if (countsMatrix.getNumberOfColumnsInRow(i) != 3) {
				continue;
			}

			String parkingNameString = countsMatrix.getString(i, 1);

			if (!parkingNames.contains(parkingNameString)) {
				parkingNames.add(parkingNameString);
			}
		}
		return parkingNames;
	}

	private static String createFileName(String parkingNameString) {
		return parkingNameString.split("/")[0].replace(" ", "") + ".png";
	}

	private static void generateParkingOccupancyPlot(StringMatrix countsMatrix, String parkingName, String outputFileName,
			Double parkingCapacity) {
		Double[] freeParkings = getNumberOfFreeParkings(countsMatrix, parkingName);

		if (containsNullEntries(freeParkings)) {
			System.out.println("Warning: '" + parkingName + "' contains null entries -> no plot was produced.");
			// ok for "Parkplatz Stadthof 11 / Wallisellenstrasse 15"
			return;
		}

		Double[] occupiedParkings = getNumberOfOccupiedParkings(freeParkings, parkingCapacity);
		plotData(occupiedParkings, "occupied Parkings", outputFileName, parkingName, "time", "occupancy");
	}

	private static boolean containsNullEntries(Double[] array) {
		for (Double num : array) {
			if (num == null) {
				return true;
			}
		}
		return false;
	}

	private static void plotData(Double[] parkingDataBins, String seriesLabel, String fileName, String title, String xLabel,
			String yLabel) {
		double matrix[][] = new double[96][1];

		if (parkingDataBins == null) {
			System.out.println();
		}

		for (int i = 0; i < 96; i++) {
			matrix[i][0] = parkingDataBins[i];
		}

		String[] seriesLabels = new String[1];
		seriesLabels[0] = seriesLabel;

		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) 4;
		}

		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}

	private static Double[] getNumberOfOccupiedParkings(Double[] freeParkings, Double parkingCapacity) {
		Double[] occupiedParkings = new Double[96];
		for (int i = 0; i < freeParkings.length; i++) {
			try {
				occupiedParkings[i] = parkingCapacity - freeParkings[i];

				if (occupiedParkings[i] < 0) {
					DebugLib.stopSystemAndReportInconsistency();
				}

			} catch (Exception e) {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}

		return occupiedParkings;
	}

	private static Double[] getNumberOfFreeParkings(StringMatrix countsData, String parkingName) {
		Double[] parkingOccupancy = new Double[96];
		boolean parkingNameFound = false;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

		for (int i = 0; i < countsData.getNumberOfRows(); i++) {
			if (countsData.getNumberOfColumnsInRow(i) != 3) {
				continue;
			}

			try {
				String dateString = countsData.getString(i, 0);
				String parkingNameString = countsData.getString(i, 1);
				String parkingOccupancyString = countsData.getString(i, 2);
				Date date = simpleDateFormat.parse(dateString);
				if (parkingNameString.startsWith(parkingName)) {
					parkingNameFound = true;

					int binIndex = date.getHours() * 4 + date.getMinutes() / 15;

					if (parkingOccupancy[binIndex] == null) {
						if (parkingOccupancyString.contains("closed")) {
							int prevBinIndex = (binIndex + 95) % 96;
							parkingOccupancy[binIndex] = parkingOccupancy[prevBinIndex];
						} else {
							parkingOccupancy[binIndex] = parseOccupancyString(parkingOccupancyString);
						}
					}
				}

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		fillupNullsWithPreviousOccupancy(parkingOccupancy);

		if (parkingNameFound) {
			return parkingOccupancy;
		} else {
			DebugLib.stopSystemAndReportInconsistency("parking not found:" + parkingName);
		}

		return null;
	}

	private static void fillupNullsWithPreviousOccupancy(Double[] parkingOccupancy) {

		// zwei runden durchgehen um schlimmsten fall abzufangen
		for (int i = 0; i < 192; i++) {
			if (parkingOccupancy[i % 96] == null) {
				int prevBinIndex = (i + 95) % 96;
				parkingOccupancy[i % 96] = parkingOccupancy[prevBinIndex];
			}
		}

	}

	private static Double parseOccupancyString(String parkingOccupancyString) {
		try {
			return Double.parseDouble(parkingOccupancyString.split("/")[1].trim());
		} catch (Exception e) {

		}

		return null;
	}

	private static HashMap<String, Double> getParkingCapacities() {
		HashMap<String, Double> parkingCapacities = new HashMap<String, Double>();

		parkingCapacities.put("Parkhaus Utoquai / Färberstrasse 6", 175.0);

		parkingCapacities.put("Parkhaus P West / Förrlibuckstrasse 151", 1000.0);

		parkingCapacities.put("Parkhaus Globus / Löwenstrasse 50", 178.0);
		parkingCapacities.put("Parkhaus Talgarten / Nüschelerstrasse 31", 110.0);
		parkingCapacities.put("Parkhaus Pfingstweid / Pfingstweidstrasse 1", 276.0);
		parkingCapacities.put("Parkhaus Hohe Promenade / Rämistrasse 22a", 499.0);
		parkingCapacities.put("Parkhaus Messe Zürich AG / Andreasstrasse 65", 2000.0);
		parkingCapacities.put("Parkhaus Nova Parking / Badenerstrasse 420", 600.0);
		// officiall nova parking has 520 parkings, but there are entries from
		// the homepage, which say that 580+ parkings are free.
		// this was verified also manually directly from the website.
		// therefore the capacity of the parking has been increased to 600
		parkingCapacities.put("Parkhaus Bleicherweg / Beethovenstrasse 35", 275.0);
		parkingCapacities.put("Parkhaus Center Eleven / Sophie-Täuber-Strasse 4", 359.0);
		parkingCapacities.put("Parkhaus Hardau II / Bullingerstrasse 73", 171.0);
		parkingCapacities.put("Parkgarage am Central / Seilergraben", 50.0);
		parkingCapacities.put("Parkhaus Dorflinde / Schwamendingenstrasse 31", 48.0);
		parkingCapacities.put("Parkhaus Zürichhorn / Dufourstrasse 142", 245.0);
		parkingCapacities.put("Parkhaus Albisriederplatz / Badenerstrasse 380", 66.0);
		parkingCapacities.put("Parkhaus Nordhaus / Dörflistrasse 120", 175.0);
		parkingCapacities.put("Parkhaus Jungholz / Jungholzstrasse 19", 139.0);
		parkingCapacities.put("Parkhaus Park Hyatt / Beethovenstrasse 21", 267.0);
		parkingCapacities.put("Parkhaus Hauptbahnhof / Sihlquai 41", 176.0);
		parkingCapacities.put("Parkhaus Stampfenbach / Niklausstrasse 1", 237.0);
		parkingCapacities.put("Parkhaus Jelmoli / Steinmühleplatz 1", 230.0);
		parkingCapacities.put("Parkhaus Urania / Uraniastrasse 3", 451.0);
		parkingCapacities.put("Parkhaus Uni Irchel / Winterthurerstrasse 181", 1000.0);
		// Also official are 588 public parkings (and 1227 total parkings).
		// the capacity has been increased to 1000 to take this into account.
		parkingCapacities.put("Parkhaus Cityport / Affolternstrasse 56", 620.0);
		parkingCapacities.put("Parkhaus Octavo / Brown-Boveri-Strasse 2", 123.0);
		parkingCapacities.put("Parkhaus Max-Bill-Platz / Armin-Bollinger-Weg", 51.0);
		parkingCapacities.put("Parkhaus Parkside / Sophie-Täuber-Strasse 10", 38.0);
		parkingCapacities.put("Parkhaus Accu / Otto-Schütz-Weg", 194.0);
		parkingCapacities.put("Parkhaus City Parking / Gessnerallee 14", 620.0);
		parkingCapacities.put("Parkhaus Feldegg / Riesbachstrasse 7", 149.0);
		parkingCapacities.put("Parkhaus Züri 11 Shopping / Nansenstrasse 5/7", 65.0);
		// official: 60, increased to 65 due to higher counts.
		parkingCapacities.put("Parkplatz Bienen / Bienen-/Herdernstrasse", 110.0);
		parkingCapacities.put("Parkplatz Eisfeld / Thurgauerstrasse 54", 240.0);
		parkingCapacities.put("Parkplatz Stadthof 11 / Wallisellenstrasse 15", 188.0);

		return parkingCapacities;
	}

	public static HashMap<String, String> getMappingOfParkingNameToParkingId() {
		HashMap<String, String> mapping = new HashMap<String, String>();

		mapping.put("Parkhaus Utoquai / Färberstrasse 6", "gp-6");

		// mapping.put("Parkhaus P West / Förrlibuckstrasse 151", "gp-7");
		// this parking has been removed, as it contains only 150 parkings in
		// the file provided by the city
		// and therefore will not be compared

		mapping.put("Parkhaus Globus / Löwenstrasse 50", "gp-27");
		mapping.put("Parkhaus Talgarten / Nüschelerstrasse 31", "gp-33");
		mapping.put("Parkhaus Pfingstweid / Pfingstweidstrasse 1", "gp-35");
		mapping.put("Parkhaus Hohe Promenade / Rämistrasse 22a", "gp-38");
		mapping.put("Parkhaus Messe Zürich AG / Andreasstrasse 65", "gp-48");
		mapping.put("Parkhaus Nova Parking / Badenerstrasse 420", "gp-49");
		mapping.put("Parkhaus Bleicherweg / Beethovenstrasse 35", "gp-51");
		mapping.put("Parkhaus Center Eleven / Sophie-Täuber-Strasse 4", "gp-54");
		mapping.put("Parkhaus Hardau II / Bullingerstrasse 73", "gp-58");
		mapping.put("Parkgarage am Central / Seilergraben", "gp-60");
		mapping.put("Parkhaus Dorflinde / Schwamendingenstrasse 31", "gp-61");
		mapping.put("Parkhaus Zürichhorn / Dufourstrasse 142", "gp-63");
		mapping.put("Parkhaus Albisriederplatz / Badenerstrasse 380", "gp-68");
		mapping.put("Parkhaus Nordhaus / Dörflistrasse 120", "gp-79");
		mapping.put("Parkhaus Jungholz / Jungholzstrasse 19", "gp-80");
		mapping.put("Parkhaus Park Hyatt / Beethovenstrasse 21", "gp-82");
		mapping.put("Parkhaus Hauptbahnhof / Sihlquai 41", "gp-83");
		mapping.put("Parkhaus Stampfenbach / Niklausstrasse 1", "gp-84");
		mapping.put("Parkhaus Jelmoli / Steinmühleplatz 1", "gp-85");
		mapping.put("Parkhaus Urania / Uraniastrasse 3", "gp-89");
		mapping.put("Parkhaus Uni Irchel / Winterthurerstrasse 181", "gp-95");
		mapping.put("Parkhaus Cityport / Affolternstrasse 56", "gp-97");
		mapping.put("Parkhaus Octavo / Brown-Boveri-Strasse 2", "gp-101");
		mapping.put("Parkhaus Max-Bill-Platz / Armin-Bollinger-Weg", "gp-106");
		mapping.put("Parkhaus Parkside / Sophie-Täuber-Strasse 10", "gp-107");
		mapping.put("Parkhaus Accu / Otto-Schütz-Weg", "gp-107");

		/**
		 * mapping not yet known for follwing parkings (need to find out by
		 * looking at map and verify capacity => see parking garages excel file)
		 */
		/*
		 * mapping.put("Parkhaus City Parking / Gessnerallee 14", null);
		 * mapping.put("Parkhaus Feldegg / Riesbachstrasse 7", null);
		 * mapping.put("Parkhaus Züri 11 Shopping / Nansenstrasse 5/7", null);
		 * mapping.put("Parkplatz Bienen / Bienen-/Herdernstrasse", null);
		 * mapping.put("Parkplatz Eisfeld / Thurgauerstrasse 54", null);
		 * mapping.put("Parkplatz Stadthof 11 / Wallisellenstrasse 15", null);
		 */
		return mapping;
	}

	/*
	 * 
	 * 
	 * TODO: add mapping table between garage parkings in xml and their names in
	 * counts log (txt data)
	 * 
	 * TODO: plot both in computer counts and counts (txt data) => could be done
	 * for all parkings to get an overview.
	 * 
	 * TODO: define a set of parkings, which to look at on an agregated level to
	 * get a better sense for the number of private parkings we need to add to
	 * the overall model/how to make garage parkings more expensive than both
	 * street and private parkings?
	 */

}
