package playground.wrashid.parkingChoice.counts;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.StringMatrixFilter;

import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;

public class Analyze {

	public static void main(String[] args) {

		File rootFolder = new File("H:/data/v-temp/parkingszh/experiment/");

		System.out.println("month\tweekDay\tlow\thigh\tcapacity");
		int capacity = 280;
		String garageName="Parkplatz Eisfeld / Thurgauerstrasse 54";

		for (File f : rootFolder.listFiles()) {
			if (!f.isDirectory()) {
				Matrix countsMatrix = GeneralLib.readStringMatrix(f.getAbsolutePath(), "\t", new RemoveNonRelevantGarageInformation(garageName));

				printLowAndHigh(countsMatrix, garageName, capacity);
			}
		}

	}
	
	

	public static void printLowAndHigh(Matrix countsMatrix, String garageName, int capacity) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

		HashMap<String, DailyLowHigh> lowHigh = new HashMap<String, Analyze.DailyLowHigh>();

		for (int i = 0; i < countsMatrix.getNumberOfRows(); i++) {
			if (countsMatrix.getNumberOfColumnsInRow(i) != 3) {
				continue;
			}

			String dateString = countsMatrix.getString(i, 0);
			String parkingNameString = countsMatrix.getString(i, 1);
			String parkingOccupancyString = countsMatrix.getString(i, 2);
			Date date = null;
			try {

				date = simpleDateFormat.parse(dateString);
			} catch (ParseException e) {
				/*
				System.out.println(dateString);
				System.out.println(parkingNameString);
				System.out.println(parkingOccupancyString);

				e.printStackTrace();
				*/
				continue;
			}
			Double freeParkingCount = SingleDayGarageParkingsCount.parseAvailableParkingString(parkingOccupancyString);

			if (parkingNameString.startsWith(garageName) && freeParkingCount != null) {
				DailyLowHigh dailyLowHigh = lowHigh.get(getDateString(date));

				if (dailyLowHigh == null) {

					dailyLowHigh = new DailyLowHigh();
					dailyLowHigh.month = date.getMonth();
					dailyLowHigh.weekDay = date.getDay();
					dailyLowHigh.capacity = capacity;
					lowHigh.put(getDateString(date), dailyLowHigh);
				}

				int occupancy = (int) Math.round(capacity - freeParkingCount);

				if (occupancy < dailyLowHigh.low) {
					dailyLowHigh.low = occupancy;
				}

				if (occupancy > dailyLowHigh.high) {
					dailyLowHigh.high = occupancy;
				}

			}

		}

		for (DailyLowHigh dailyLowHigh : lowHigh.values()) {
			System.out.println(dailyLowHigh.month + "\t" + dailyLowHigh.weekDay + "\t" + dailyLowHigh.low + "\t"
					+ dailyLowHigh.high + "\t" + dailyLowHigh.capacity);
		}

	}

	private static class DailyLowHigh {
		int month;
		int weekDay;
		int capacity;
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
	}

	private static String getDateString(Date date) {
		return date.getDate() + "." + date.getMonth() + 1 + "." + date.getYear() + 1900;
	}

}
