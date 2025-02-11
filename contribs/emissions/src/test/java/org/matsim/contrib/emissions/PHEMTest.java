package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.math.IEEE754rUtils.max;
import static org.apache.commons.lang3.math.IEEE754rUtils.min;

public class PHEMTest {

	// TODO Rename CO to CO(total)
	// TODO Rename PMx to PM

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO Files were changed to local for debugging purposes. CHange them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "D:/Projects/VSP/MATSim/PHEM/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	// TODO DEBUG ONLY
	static CSVPrinter csvPrinter = null;
	FileWriter fileWriter = null;

	// ----- Helper methods -----

	private static List<DrivingCycleSecond> readCycle(Path path){
		List<DrivingCycleSecond> drivingCycleSeconds = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(";")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		try (var reader = Files.newBufferedReader(path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				drivingCycleSeconds.add(new DrivingCycleSecond(
					Integer.parseInt(record.get(0)),
					Double.parseDouble(record.get(1)),
					Double.parseDouble(record.get(2))));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return drivingCycleSeconds;
	}

	/** TODO Remove
	 * Reads a csv (header: time,speed,acceleration) containing a wltp cycle and converts it into a MATSim test-network, with links representing the
	 * phases of the cycle. The first link has its origin at (0,0) and the test-track extends into positive x.
	 * @param network Network to put in the links
	 * @param path path of the wltp.csv
	 */
	private static void createTestLinks(Network network, Path path){
		List<DrivingCycleSecond> drivingCycleSeconds = readCycle(path);
		for (var i : drivingCycleSeconds) {
			System.out.println(i);
		}

		/*
		Partitions the drivingCycleSeconds list into multiple disjunct lists, containing consecutive records.
		A new partition is created at every minimum. The minimum is determined by computing the delta value.
		If the sign changed form negative to positive, the minimum must be somewhere between this point and the last.
		NOTE: We are not using the acceleration as the data does not match numerically with the velocity.
		 */
		List<List<DrivingCycleSecond>> drivingSegments = new ArrayList<>();
		List<DrivingCycleSecond> currentList = new ArrayList<>();
		double lastDelta = 1;
		for (int sec = 1; sec < drivingCycleSeconds.size(); sec++) {
			double currentDelta = drivingCycleSeconds.get(sec).vel - drivingCycleSeconds.get(sec - 1).vel;

			/*
			We want to create a new link at
			1. every minimum
			2. every end of a standing period
			TODO currently, it is impossible to simulate standing times. How do we want to overcome this?
			TODO -> currently, the program creates links with 0 speed and length, which will probably cause problems
			 */
			if ((lastDelta <= 0 && currentDelta > 0)) {
				// We have found a minimum, crate a new sublist
				drivingSegments.add(currentList);
				currentList = new ArrayList<>();

				// TODO DBEUG ONLY
				try{
					csvPrinter.printRecord(drivingCycleSeconds.get(sec).second);
				} catch(Exception e){
					System.out.println(e);
				}

			}

			lastDelta = currentDelta;

			currentList.add(drivingCycleSeconds.get(sec));
		}

		// Add last segment
		drivingSegments.add(currentList);


		// Now create a link for each driving segment
		int i = 1;
		double dist_summed = 0;
		Node from_node = NetworkUtils.createAndAddNode(network, Id.createNodeId("n0"), new Coord(0, 0));
		for (var segment : drivingSegments) {
			// Compute the sum of the segment (which equals the numerical integral, thus the total distance)
			double len = segment.stream().map(s -> s.vel/3.6).reduce(0., Double::sum);
			double freespeed = (len / (segment.getLast().second+1 - segment.getFirst().second));

			Node to_node = NetworkUtils.createAndAddNode(network, Id.createNodeId("n" + i), new Coord(dist_summed + len, 0));
			Link link = NetworkUtils.createLink(Id.createLinkId("l" + i), from_node, to_node, network, len, freespeed, 10000, 1);
			network.addLink(link);
			from_node = to_node;
			dist_summed += len;
			i++;
		}
	}

	private static List<SumoEntry> readSumoFile(Path path){
		List<SumoEntry> sumoSeconds = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(";")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		try (var reader = Files.newBufferedReader(path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				sumoSeconds.add(new SumoEntry(
					SumoEntry.Type.SECOND,
					Integer.parseInt(record.get(0)),
					Double.parseDouble(record.get(1)),
					Double.parseDouble(record.get(2)),
					Double.parseDouble(record.get(3)),
					Double.parseDouble(record.get(4)),
					Double.parseDouble(record.get(5)),
					Double.parseDouble(record.get(6)),
					Double.parseDouble(record.get(7)),
					Double.parseDouble(record.get(8)),
					Double.parseDouble(record.get(9)),
					Double.parseDouble(record.get(10))
				));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}

		return sumoSeconds;
	}

	/**
	 * Read a SUMO emissionsDrivingCycle csv-output and maps the emissions to the test links, given in {@code wltpLinkAttributes}.
	 * @param path Path to SUMO output-file
	 * @param wltpLinkAttributes Array of records, containing the information of each WLTP-test-segment
	 * @return a {@link SumoEntry} for each WLTP-test-segment
	 */
	private static List<SumoEntry> readSumoEmissionsForLinks(Path path, List<WLTPLinkAttributes> wltpLinkAttributes){
		List<SumoEntry> sumoSeconds = readSumoFile(path);
		List<SumoEntry> sumoSegments = new ArrayList<>();

		/*
		Partition the SumoSeconds into the segments, given in wltpLinkAttributes.
		 */
		int currentSecond = 0;
		for(WLTPLinkAttributes wltpLink : wltpLinkAttributes){
			int travelTime = wltpLink.time;
			double sumVelocity = 0;
			double sumAcceleration = 0;
			double sumSlope = 0;
			double sumCO = 0;
			double sumCO2 = 0;
			double sumHC = 0;
			double sumPMx = 0;
			double sumNOx = 0;
			double sumFuel = 0;
			double sumElectricity = 0;

			int i = currentSecond;
			for(; i < currentSecond + wltpLink.time; i++){
				sumVelocity += sumoSeconds.get(i).velocity;
				sumAcceleration += sumoSeconds.get(i).acceleration;
				sumSlope += sumoSeconds.get(i).slope;
				sumCO += sumoSeconds.get(i).CO;
				sumCO2 += sumoSeconds.get(i).CO2;
				sumHC += sumoSeconds.get(i).HC;
				sumPMx += sumoSeconds.get(i).PMx;
				sumNOx += sumoSeconds.get(i).NOx;
				sumFuel += sumoSeconds.get(i).fuel;
				sumElectricity += sumoSeconds.get(i).electricity;
			}

			currentSecond = i;

			sumoSegments.add(new SumoEntry(
				SumoEntry.Type.SEGMENT,
				travelTime,
				sumVelocity/travelTime,
				sumAcceleration/travelTime,
				sumSlope/travelTime,
				sumCO,
				sumCO2,
				sumHC,
				sumPMx,
				sumNOx,
				sumFuel,
				sumElectricity
			));
		}

		//Make sure, that everything went right
		assert sumoSegments.size() == wltpLinkAttributes.size();

		return sumoSegments;
	}

	private static List<WLTPLinkComparison> readReferenceComparison(Path path){
		List<WLTPLinkComparison> refComparison = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(",")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		try (var reader = Files.newBufferedReader(path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				refComparison.add(new WLTPLinkComparison(
					Integer.parseInt(record.get(0)),
					Integer.parseInt(record.get(1)),
					Integer.parseInt(record.get(2)),

					new double[]{
						Double.parseDouble(record.get(3)),
						Double.parseDouble(record.get(4)),
						Double.parseDouble(record.get(5)),
						Double.parseDouble(record.get(6))
					},

					new double[]{
						Double.parseDouble(record.get(7)),
						Double.parseDouble(record.get(8)),
						Double.parseDouble(record.get(9)),
						Double.parseDouble(record.get(10))
					},

					new double[]{
						Double.parseDouble(record.get(11)),
						Double.parseDouble(record.get(12)),
						Double.parseDouble(record.get(13)),
						Double.parseDouble(record.get(14))
					},

					new double[]{
						Double.parseDouble(record.get(15)),
						Double.parseDouble(record.get(16)),
						Double.parseDouble(record.get(17)),
						Double.parseDouble(record.get(18))
					},

					new double[]{
						Double.parseDouble(record.get(19)),
						Double.parseDouble(record.get(20)),
						Double.parseDouble(record.get(21)),
						Double.parseDouble(record.get(22))
					}
				));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}

		return refComparison;
	}

	// ----- Sub-Test methods -----

	/**
	 * Tests if the values match the HBEFA v4.1 reference values exactly.
	 * <i>NOTE: This test will most likely fail if a new HBEFA version is used. The purpose if this test is to remind you of any result-changes
	 * when switching to a new version. If the new results seem reasonable, you can override the reference file.</i>
	 */
	private void testHbefaV4_1(List<WLTPLinkComparison> refComparison, List<WLTPLinkComparison> testComparison) {
		 Assertions.assertEquals(refComparison.size(), testComparison.size(), "It seems like you are comparing two different setups. Maybe the reference-file was changed improperly?");

		for (int i = 0; i < refComparison.size(); i++){
			Assertions.assertEquals(refComparison.get(i).CO[1], testComparison.get(i).CO[1], 0.001, "MATSim values changed for CO");
			Assertions.assertEquals(refComparison.get(i).CO2[1], testComparison.get(i).CO2[1], 0.001, "MATSim values changed for CO2");
			Assertions.assertEquals(refComparison.get(i).HC[1], testComparison.get(i).HC[1], 0.001, "MATSim values changed for HC");
			Assertions.assertEquals(refComparison.get(i).PMx[1], testComparison.get(i).PMx[1], 0.001, "MATSim values changed for PMx");
			Assertions.assertEquals(refComparison.get(i).NOx[1], testComparison.get(i).NOx[1], 0.001, "MATSim values changed for NOx");
		}
	}

	/**
	 * Tests if the results lie in the allowed deviation interval, which is the range between the reference SUMO and MATSim emissions.
	 * TODO CHeck if this test-setup is really useful. What if the deviation-factor decreases, but the sign of difference switches, so it lies outside?
	 * @param refComparison original reference values
	 * @param testComparison test values
	 */
	private void testValueDeviation(List<WLTPLinkComparison> refComparison, List<WLTPLinkComparison> testComparison){
		for (int i = 0; i < refComparison.size(); i++){
			// Check CO interval
			Assertions.assertTrue(testComparison.get(i).CO[1] >= min(refComparison.get(i).CO[0], refComparison.get(i).CO[1]));
			Assertions.assertTrue(testComparison.get(i).CO[1] <= max(refComparison.get(i).CO[0], refComparison.get(i).CO[1]));
			// Check CO2 interval
			Assertions.assertTrue(testComparison.get(i).CO2[1] >= min(refComparison.get(i).CO2[0], refComparison.get(i).CO2[1]));
			Assertions.assertTrue(testComparison.get(i).CO2[1] <= max(refComparison.get(i).CO2[0], refComparison.get(i).CO2[1]));
			// Check HC interval
			Assertions.assertTrue(testComparison.get(i).HC[1] >= min(refComparison.get(i).HC[0], refComparison.get(i).HC[1]));
			Assertions.assertTrue(testComparison.get(i).HC[1] <= max(refComparison.get(i).HC[0], refComparison.get(i).HC[1]));
			// Check PMx interval
			Assertions.assertTrue(testComparison.get(i).PMx[1] >= min(refComparison.get(i).PMx[0], refComparison.get(i).PMx[1]));
			Assertions.assertTrue(testComparison.get(i).PMx[1] <= max(refComparison.get(i).PMx[0], refComparison.get(i).PMx[1]));
			// Check NOx interval
			Assertions.assertTrue(testComparison.get(i).NOx[1] >= min(refComparison.get(i).NOx[0], refComparison.get(i).NOx[1]));
			Assertions.assertTrue(testComparison.get(i).NOx[1] <= max(refComparison.get(i).NOx[0], refComparison.get(i).NOx[1]));
		}
	}

	// TODO Petrol and diesel
	@ParameterizedTest
	@ValueSource(strings = {"petrol", "diesel"}) // TODO Add diesel
	public void test(String fuel) throws IOException, URISyntaxException {
		// Prepare emission-config
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction ); //TODO Check that this is correct
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ); //TODO Check that this is correct
		ecg.setHbefaConsistencyChecker(EmissionsConfigGroup.UseHbefaConsistencyChecker.skip); // TODO Remove this
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		// Create config
		Config config = ConfigUtils.createConfig(ecg);

		// Define Test-case (WLTP Class 3b) -> Creates a link for each wltp-segment
		List<WLTPLinkAttributes> wltpLinkAttributes = new ArrayList<>();
		wltpLinkAttributes.add(new WLTPLinkAttributes(589, 3095, 15.69, "URB/Local/50"));
		wltpLinkAttributes.add(new WLTPLinkAttributes(433, 4756, 21.28, "URB/MW-City/80"));
		wltpLinkAttributes.add(new WLTPLinkAttributes(455, 7158, 27.06, "RUR/MW/100"));
		wltpLinkAttributes.add(new WLTPLinkAttributes(323, 8254, 36.47, "RUR/MW/130"));

		Path dir = Paths.get(utils.getClassInputDirectory()).resolve("sumo_" + fuel + "_output.csv");
		List<PHEMTest.SumoEntry> sumoSegments = readSumoEmissionsForLinks(dir, wltpLinkAttributes);

		// Define vehicle
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		switch(fuel){
			case "petrol":
				vehicleAttributes.setHbefaTechnology("petrol (4S)");
				vehicleAttributes.setHbefaEmConcept("PC P Euro-4");
				break;
			case "diesel":
				vehicleAttributes.setHbefaTechnology("diesel");
				vehicleAttributes.setHbefaEmConcept("PC D Euro-4");
				break;
		}
		vehicleAttributes.setHbefaSizeClass("average"); // TODO Fix table for final tests
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = new Tuple<>(
			HbefaVehicleCategory.PASSENGER_CAR,
			vehicleAttributes);

		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		// Calculate MATSim-emissions
		EmissionModule module = new EmissionModule(scenario, manager);
		List<Map<Pollutant, Double>> link_pollutant2grams = new ArrayList<>();
		for (WLTPLinkAttributes wltpLinkAttribute : wltpLinkAttributes) {
			link_pollutant2grams.add(module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
				wltpLinkAttribute.time,
				wltpLinkAttribute.hbefaStreetType,
				wltpLinkAttribute.freespeed,
				wltpLinkAttribute.length,
				vehHbefaInfo));
		}

		/*var file = Paths.get("C:\\Users\\aleks\\Desktop\\aggregate.csv");
		try(var bufWriter = Files.newBufferedWriter(file); var csv = CSVFormat.DEFAULT.print(bufWriter)) {
			for (var i = 0; i < wltpLinkAttributes.length; i++) {
				var length_m = wltpLinkAttributes[i].length;
				var matsim_em = link_pollutant2grams.get(i);
				var sumo_em = sumoSegments.get(i).NOx();
				var matsim_em_g_km = matsim_em.get(Pollutant.NOx) / ((double) length_m / 1000);
				var sumo_em_g_km = (sumo_em/1000) / ((double) length_m / 1000);
				System.out.println("Link " + i + ": MATSim NOx=" + matsim_em_g_km + " g/km, SUMO NOx=" + sumo_em_g_km + " g/km");
			}
		}*/
		// Now we need to read in the sumo-files and get the SUMO results
		// output-files comes from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html

		// Now we have everything we need for comparing -> Compute the difference between MATSim- and SUMO-emissions

		List<WLTPLinkComparison> comparison = new ArrayList<>();
		int currentSecond = 0;
		for(int i = 0; i < wltpLinkAttributes.size(); i++){
			comparison.add( new WLTPLinkComparison(
				i,
				currentSecond,
				wltpLinkAttributes.get(i).time,

				new double[]{sumoSegments.get(i).CO/1000,
					link_pollutant2grams.get(i).get(Pollutant.CO),
					link_pollutant2grams.get(i).get(Pollutant.CO) - sumoSegments.get(i).CO/1000,
					link_pollutant2grams.get(i).get(Pollutant.CO) / (sumoSegments.get(i).CO/1000)},

				new double[]{sumoSegments.get(i).CO2/1000,
					link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL),
					link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL) - sumoSegments.get(i).CO2/1000,
					link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL) / (sumoSegments.get(i).CO2/1000)},

				new double[]{sumoSegments.get(i).HC/1000,
					link_pollutant2grams.get(i).get(Pollutant.HC),
					link_pollutant2grams.get(i).get(Pollutant.HC) - sumoSegments.get(i).HC/1000,
					link_pollutant2grams.get(i).get(Pollutant.HC) / (sumoSegments.get(i).HC/1000)},

				new double[]{sumoSegments.get(i).PMx/1000,
					link_pollutant2grams.get(i).get(Pollutant.PM),
					link_pollutant2grams.get(i).get(Pollutant.PM) - sumoSegments.get(i).PMx/1000,
					link_pollutant2grams.get(i).get(Pollutant.PM) / (sumoSegments.get(i).PMx/1000)},

				new double[]{sumoSegments.get(i).NOx/1000,
					link_pollutant2grams.get(i).get(Pollutant.NOx),
					link_pollutant2grams.get(i).get(Pollutant.NOx) - sumoSegments.get(i).NOx/1000,
					link_pollutant2grams.get(i).get(Pollutant.NOx) / (sumoSegments.get(i).NOx/1000)}
			));
			currentSecond += wltpLinkAttributes.get(i).time;
		}

		// Print out the results as csv
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter(utils.getOutputDirectory() + "diff_" + fuel + "_out.csv"),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"segment",
			"startTime",
			"travelTime",
			"CO-SUMO",
			"CO-MATSIM",
			"CO-Diff",
			"CO-Factor",
			"CO2(total)-SUMO",
			"CO2(total)-MATSIM",
			"CO2(total)-Diff",
			"CO2(total)-Factor",
			"HC-SUMO",
			"HC-MATSIM",
			"HC-Diff",
			"HC-Factor",
			"PM-SUMO",
			"PM-MATSIM",
			"PM-Diff",
			"PM-Factor",
			"NOx-SUMO",
			"NOx-MATSIM",
			"NOx-Diff",
			"NOx-Factor"
		);

		for(int i = 0; i < comparison.size(); i++){
			writer.printRecord(
				i,
				comparison.get(i).startTime,
				comparison.get(i).travelTime,

				comparison.get(i).CO[0],
				comparison.get(i).CO[1],
				comparison.get(i).CO[2],
				comparison.get(i).CO[3],

				comparison.get(i).CO2[0],
				comparison.get(i).CO2[1],
				comparison.get(i).CO2[2],
				comparison.get(i).CO2[3],

				comparison.get(i).HC[0],
				comparison.get(i).HC[1],
				comparison.get(i).HC[2],
				comparison.get(i).HC[3],

				comparison.get(i).PMx[0],
				comparison.get(i).PMx[1],
				comparison.get(i).PMx[2],
				comparison.get(i).PMx[3],

				comparison.get(i).NOx[0],
				comparison.get(i).NOx[1],
				comparison.get(i).NOx[2],
				comparison.get(i).NOx[3]
			);
		}
		writer.flush();
		writer.close();

		// Start the tests
		var refComparison = readReferenceComparison(Paths.get(utils.getClassInputDirectory()).resolve("diff_" + fuel + "_ref.csv"));
		testHbefaV4_1(refComparison, comparison);
		testValueDeviation(refComparison, comparison);

	}

	private record DrivingCycleSecond(int second, double vel, double acc){}

	/**
	 * Represents a row of the SUMO output-file / a segment of the WLTP-cycle.
	 * The enum defines which of them it represents.
	 * @param type wheter this rentry represents a second / segment
	 * @param second start time in [s] / travel time in [s]
	 * @param velocity [m/s] / avg velocity [m/s]
	 * @param acceleration [m/s^2] / avg acceleration [m/s^2]
	 * @param slope [°] / avg slope [°]
	 * @param CO [mg/s] / [mg]
	 * @param CO2 [mg/s] / [mg]
	 * @param HC [mg/s] / [mg]
	 * @param PMx [mg/s] / [mg]
	 * @param NOx [mg/s] / [mg]
	 * @param fuel [mg/s] / [mg]
	 * @param electricity [Wh/s] / [Wh]
	 */
	private record SumoEntry(SumoEntry.Type type, int second, double velocity, double acceleration, double slope, double CO, double CO2, double HC, double PMx, double NOx, double fuel, double electricity){
		private enum Type{
			SECOND,
			SEGMENT
		}
	}

	/**
	 * Represents a segment of the WLTP-cycle. Each segment would be one link in MATSim.
	 * @param time travelTime [s]
	 * @param length [m]
	 * @param freespeed [m/s]
	 * @param hbefaStreetType More information at: {@link VspHbefaRoadTypeMapping}
	 */
	private record WLTPLinkAttributes(int time, int length, double freespeed, String hbefaStreetType){}

	/**
	 * Helper struct containing values important for comparing SUMO and MATSim emissions.
	 * CO, CO2, HC, PM, NO are double-arrays of length 4, containing: absolute emission SUMO / absolute emission MATSim / diff / factor
	 * @param segment index of segment
	 * @param startTime in seconds
	 * @param travelTime in seconds <br>
	 */
	private record WLTPLinkComparison(int segment, int startTime, int travelTime, double[] CO, double[] CO2, double[] HC, double[] PMx, double[] NOx){}
}
