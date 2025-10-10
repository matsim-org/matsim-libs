package playground.vsp.drt.accessibilityOrientedDrt.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.population.PopulationUtils;
import playground.vsp.drt.accessibilityOrientedDrt.alternativeMode.AlternativeModeTripData;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.PassengerAttribute;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static playground.vsp.drt.accessibilityOrientedDrt.alternativeMode.AlternativeModeTripData.readData;

public class SystemPerformanceAnalysis {

	public static void analyzeSystemPerformance(String outputDirectory, Map<String, AlternativeModeTripData> alternativeModeTripDataMap,
												Map<String, String> personAttributeMap) throws IOException {
		// prepare result writer
		CSVPrinter systemPerformancePrinter = new CSVPrinter(new FileWriter(outputDirectory + "/system-performance-analysis.tsv"), CSVFormat.TDF);
		systemPerformancePrinter.printRecord("id", "departure_time", "direct_car_travel_time", "actual_total_travel_time", "mode",
			"passenger_type", "remark");

		// collect all the served DRT trips
		Path servedDrtTrips = ApplicationUtils.globFile(Path.of(outputDirectory), "*output_drt_legs_drt.csv*");
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(servedDrtTrips),
			CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).get())) {
			for (CSVRecord record : parser.getRecords()) {
				// get total travel time
				String personIdString = record.get("personId");
				String passengerType = personAttributeMap.get(personIdString);
				double directTravelTime = alternativeModeTripDataMap.get(personIdString).directCarTravelTime();
				double departureTime = Double.parseDouble(record.get("submissionTime"));
				double waitTime = Double.parseDouble(record.get("waitTime"));
				double inVehicleTravelTime = Double.parseDouble(record.get("inVehicleTravelTime"));
				double totalTravelTime = waitTime + inVehicleTravelTime;

				systemPerformancePrinter.printRecord(
					personIdString,
					Double.toString(departureTime),
					Double.toString(directTravelTime),
					Double.toString(totalTravelTime),
					TransportMode.drt,
					passengerType,
					"served"
				);
			}
		}

		// collect all the unserved (rejected) trips → assume alternative mode is used
		Path rejectedTrips = ApplicationUtils.globFile(Path.of(outputDirectory), "*output_drt_rejections_drt.csv*");
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(rejectedTrips),
			CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).get())) {
			for (CSVRecord record : parser.getRecords()) {
				String personIdString = record.get("personIds");
				String passengerType = personAttributeMap.get(personIdString);
				double departureTime = Double.parseDouble(record.get("time"));
				double directTravelTime = alternativeModeTripDataMap.get(personIdString).directCarTravelTime();
				double totalTravelTime = alternativeModeTripDataMap.get(personIdString).actualTotalTravelTime();
				String mode = alternativeModeTripDataMap.get(personIdString).mode();

				systemPerformancePrinter.printRecord(
					personIdString,
					Double.toString(departureTime),
					Double.toString(directTravelTime),
					Double.toString(totalTravelTime),
					mode,
					passengerType,
					"rejected"
				);
			}
		}

		// close the printer and write down the result
		systemPerformancePrinter.close();
	}

	// run offline analysis: args: output folder, path to alternative data
	public static void main(String[] args) throws IOException {
		Map<String, AlternativeModeTripData> alternativeModeTripDataMap = new HashMap<>();
		Map<String, String> personAttributeMap = new HashMap<>();

		Path outputPlans = ApplicationUtils.globFile(Path.of(args[0]), "*output_plans.xml.gz*");
		Population plans = PopulationUtils.readPopulation(outputPlans.toString());
		for (Person person : plans.getPersons().values()) {
			personAttributeMap.put(person.getId().toString(), person.getAttributes().getAttribute(PassengerAttribute.ATTRIBUTE_NAME).toString());
		}

		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(args[1])),
			CSVFormat.TDF.withFirstRecordAsHeader())) {
			for (CSVRecord record : parser.getRecords()) {
				AlternativeModeTripData alternativeModeTripData = readData(record);
				alternativeModeTripDataMap.put(alternativeModeTripData.id(), alternativeModeTripData);
			}
		}
		analyzeSystemPerformance(args[0], alternativeModeTripDataMap, personAttributeMap);
	}
}
