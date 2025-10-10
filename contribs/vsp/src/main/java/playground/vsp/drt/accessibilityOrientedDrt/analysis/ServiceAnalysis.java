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
		double maxObservedWaitingTime;
		Path customerStatsPath = ApplicationUtils.globFile(Path.of(outputFolder), "*drt_customer_stats_drt.csv*");
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(customerStatsPath),
			CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).get())) {
			CSVRecord lastRecord = null;
			for (CSVRecord record : parser.getRecords()) {
				lastRecord = record;
			}
			// get information from the last records
			assert lastRecord != null;
//			maxObservedWaitingTime = Double.parseDouble(lastRecord.get("wait_max"));

			// Alternatively, we can also use 95-pct value
			maxObservedWaitingTime = Double.parseDouble(lastRecord.get("wait_p95"));

		}

		assert maxObservedWaitingTime >= 0;
		return maxObservedWaitingTime <= maxAllowedWaitingTime;

		// (b) check the max travel time constraint
		// TODO For now we only check the waiting time constraint. The checking of total travel time is to be implemented later.
		// Because the violation of arrival time has a much higher penalty, this means if the waiting time constraint is fulfilled, then the
		// total travel time constraint is also likely to be fulfilled. (So it should be fine for the time being)

	}
}
