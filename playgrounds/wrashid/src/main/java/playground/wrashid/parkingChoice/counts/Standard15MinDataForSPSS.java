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

public class Standard15MinDataForSPSS {

	public static void main(String[] args) {
		File rootFolder = new File("H:/data/v-temp/parkingszh/experiment/");

		System.out.println("parkingName\tyear\tmonth\tweekDay\t15minBin\tparkingAvailable");
		WriteOutRelevantInformation wori = new WriteOutRelevantInformation();

		for (File f : rootFolder.listFiles()) {
			if (!f.isDirectory()) {
				Matrix countsMatrix = GeneralLib.readStringMatrix(f.getAbsolutePath(), "\t", wori);
			}
		}

	}

	public static void process(Matrix countsMatrix) {

	}

	public static class WriteOutRelevantInformation implements StringMatrixFilter {

		HashMap<String, Integer> currentTimeBin = new HashMap<String, Integer>();

		@Override
		public boolean removeLine(String line) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

			String[] str = line.split("\t");

			if (str.length != 3) {
				return true;
			}

			String dateString = str[0];
			String parkingName = str[1];
			Double parkingAvailable = SingleDayGarageParkingsCount.parseAvailableParkingString(str[2]);
			Date date = null;
			try {

				date = simpleDateFormat.parse(dateString);
			} catch (ParseException e) {

				return true;
			}

			if (parkingAvailable == null) {
				return true;
			}

			Integer timeBin = ((date.getHours() * 60 + date.getMinutes()) / 15);

			if (currentTimeBin.get(parkingName) == null || currentTimeBin.get(parkingName) != timeBin) {
				System.out.println(parkingName + "\t" + (date.getYear() + 1900) + "\t" + (date.getMonth() + 1) + "\t"
						+ date.getDay() + "\t" + timeBin + "\t" + Math.round(parkingAvailable));

				currentTimeBin.put(parkingName, timeBin);
			}

			return true;
		}

	}

}
