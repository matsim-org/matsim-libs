package playground.vsp.drt.accessibilityOrientedDrt.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.ApplicationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServiceAnalysis {

	public static boolean isServiceSatisfactory(String outputFolder, double maxAllowedWaitingTime) throws IOException {
		// check whether the fleet size is adequate
		// (a) check the waiting time constraint
		double waitingTimeStats;
		Path customerStatsPath = ApplicationUtils.globFile(Path.of(outputFolder), "*drt_customer_stats_drt.csv*");
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(customerStatsPath),
			CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).get())) {
			CSVRecord lastRecord = null;
			for (CSVRecord record : parser.getRecords()) {
				lastRecord = record;
			}
			// get information from the last records
			assert lastRecord != null;
//			waitingTimeStats = Double.parseDouble(lastRecord.get("wait_max"));

			// Alternatively, we can also use 95-pct value
			waitingTimeStats = Double.parseDouble(lastRecord.get("wait_p95"));

		}
		assert waitingTimeStats >= 0;

		// (b) check the max travel time constraint
		Path drtLegPath = ApplicationUtils.globFile(Path.of(outputFolder), "*output_drt_legs_drt.csv*");
		double punctualArrivals = 0.;
		double numDrtTrips = 0.;
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(drtLegPath),
			CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).get())) {
			for (CSVRecord record : parser.getRecords()) {
				numDrtTrips++;
				if ((Double.parseDouble(record.get("latestArrivalTime")) + 1 >= Double.parseDouble(record.get("arrivalTime")))) {
					punctualArrivals++;
				}
			}
		}
		if (numDrtTrips == 0) {
			return true;
		}
		double punctualArrivalRate = punctualArrivals / numDrtTrips;

		// if both requirements are fulfilled, then return true (i.e., fleet size is adequate)
		return waitingTimeStats <= maxAllowedWaitingTime && punctualArrivalRate >= 0.95;
	}
}
