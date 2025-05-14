package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.commons.lang3.math.IEEE754rUtils.max;
import static org.apache.commons.lang3.math.IEEE754rUtils.min;

/**
 * This class contains three tests, that differ in levels of strictness: <br>
 *  (1) Values must be exact to the results of HBEFAv4.1. This test should not fail, when changes are done in the emission-contrib.
 *  	It will however fail, if a new HBEFA-version is set up. <br>
 *  (2)	Values lie between SUMO and HBEFAv4.1. This test may fail when setting up a new HBEFA-version. Affected values should be checked <br>
 *  (3) Average-deviance is better than for HBEFAv4.1. This test should never fail, even when setting up a new HBEFA-version. <br>
 *  If you need to adjust this test for future HBEFA versions, you can use the diff_{fuel}_out.csv output and change and sue it as new reference file.
 *  The sumo_{fuel}_output.csv does not need to be changed, unless the PHEMLight engine got an update.
 */
public class PHEMTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO Files were changed to local for debugging purposes. CHange them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "D:/Projects/VSP/MATSim/PHEM/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	static CSVPrinter csvPrinter = null;

	// ----- Helper methods -----

	/**
	 * Reads out a csv-file containing a driving cycle.
	 */
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

	/**
	 * TODO Add different constraint options when to add a new link
	 * Reads a csv (header: time,speed,acceleration) containing a wltp cycle and converts it into a List og {@link WLTPLinkAttributes},
	 * with each element representing a section of the cycle.
	 * @param path path of the wltp.csv
	 * @param cutSetting defining where the cuts in the cycle are set
	 */
	private static List<WLTPLinkAttributes> createTestLinks(Path path, LinkCutSetting cutSetting){
		List<DrivingCycleSecond> drivingCycleSeconds = readCycle(path);
		for (var i : drivingCycleSeconds) {
			System.out.println(i);
		}

		/*
		Partitions the drivingCycleSeconds list into multiple disjunct lists, containing consecutive records.
		When a new partition is created depends on the curSetting.
		 */
		List<List<DrivingCycleSecond>> drivingSegments = new ArrayList<>();
		List<DrivingCycleSecond> currentList = new ArrayList<>();
		double lastDelta = 1;
		double lastTime = 0;
		for (int sec = 1; sec < drivingCycleSeconds.size(); sec++) {
			double currentDelta = drivingCycleSeconds.get(sec).vel - drivingCycleSeconds.get(sec - 1).vel;


			if(cutSetting == LinkCutSetting.fixedIntervalLength){
				/*
				We want to create a new link at
				1. Each end of fixed interval
				 */
				if(sec % cutSetting.getAttr() == 0){
					drivingSegments.add(currentList);
					currentList = new ArrayList<>();
				}
			} else if(cutSetting == LinkCutSetting.eachMinimum){
				/*
				We want to create a new link at
				1. every minimum
				2. every end of a standing period
				The minimum is determined by computing the delta value.
				If the sign changed form negative to positive, the minimum must be somewhere between this point and the last.
				NOTE: We are not using the acceleration as the data does not match numerically with the velocity.
				TODO currently, it is impossible to simulate standing times. How do we want to overcome this?
				TODO -> currently, the program creates links with 0 speed and length, which will probably cause problems
				 */

				if (lastDelta <= 0 && currentDelta > 0) {
					// We have found a minimum, crate a new sublist
					drivingSegments.add(currentList);
					currentList = new ArrayList<>();

					/*// TODO DEBUG ONLY
					try{
						csvPrinter.printRecord(drivingCycleSeconds.get(sec).second);
					} catch(Exception e){
						System.out.println(e);
					*/
				}
			} else if(cutSetting == LinkCutSetting.minSpacingMinimum){
				/*
				We want to create a new link at
				1. every minimum
				2. every end of a standing period
				But only if the last end of a segment was at least 'spacing'-seconds before.

				TODO currently, it is impossible to simulate standing times. How do we want to overcome this?
				TODO -> currently, the program creates links with 0 speed and length, which will probably cause problems
				 */

				if (lastDelta <= 0 && currentDelta > 0 && sec - lastTime > cutSetting.getAttr()) {
					// We have found a minimum, crate a new sublist
					drivingSegments.add(currentList);
					currentList = new ArrayList<>();
					lastTime = sec;
				}
			} else {
				throw new RuntimeException("LinkCutSetting " + cutSetting.name() + " not implemented!");
			}

			lastDelta = currentDelta;

			currentList.add(drivingCycleSeconds.get(sec));
		}

		// Add last segment
		drivingSegments.add(currentList);

		// Now build the list of WLTP Link attributes
		VspHbefaRoadTypeMapping mapping = new VspHbefaRoadTypeMapping();

		// Variables needed for the mapping
		List<WLTPLinkAttributes> attributeList = new ArrayList<>();
		Node n0 = NetworkUtils.createNode(Id.createNodeId("n0"));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("n0"));

		int i = 1;
		double dist_summed = 0;
		for (var segment : drivingSegments) {
			// Compute the sum of velocities of the segment (which equals the numerical integral, thus the total distance)
			double len = segment.stream().map(s -> s.vel/3.6).reduce(0., Double::sum);

			// TODO This is a temporary solution. Make this work properly
			if(len == 0)
				len = 1;

			double freespeed = (len / (segment.getLast().second+1 - segment.getFirst().second));

			// Use default hbefa Hbefa Road Type mapping
			Link link = NetworkUtils.createLink(Id.createLinkId("l" + i), null, null, null, len, freespeed, 10000, 1);
			String roadType = mapping.determineHbefaType(link);

			attributeList.add(new WLTPLinkAttributes(segment.size(), len, freespeed, roadType));

			dist_summed += len;
			i++;
		}

		return attributeList;

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
					Double.parseDouble(record.get(3)),

					new double[]{
						Double.parseDouble(record.get(4)),
						Double.parseDouble(record.get(5)),
						Double.parseDouble(record.get(6)),
						Double.parseDouble(record.get(7))
					},

					new double[]{
						Double.parseDouble(record.get(8)),
						Double.parseDouble(record.get(9)),
						Double.parseDouble(record.get(10)),
						Double.parseDouble(record.get(11))
					},

					new double[]{
						Double.parseDouble(record.get(12)),
						Double.parseDouble(record.get(13)),
						Double.parseDouble(record.get(14)),
						Double.parseDouble(record.get(15))
					},

					new double[]{
						Double.parseDouble(record.get(16)),
						Double.parseDouble(record.get(17)),
						Double.parseDouble(record.get(18)),
						Double.parseDouble(record.get(19))
					},

					new double[]{
						Double.parseDouble(record.get(20)),
						Double.parseDouble(record.get(21)),
						Double.parseDouble(record.get(22)),
						Double.parseDouble(record.get(23))
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
	 * Tests if the results lie in the allowed deviation interval, which is the range between the reference SUMO and MATSim emissions. <br>
	 * <i>NOTE: This test could fail, even if overall results got better. If this test fails, after setting up a new HBEFA-version, the values could
	 * have been "overcorrected" so that the difference was inverted. This is not necessarily bad, but should be investigated.</i>
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

	/**
	 * Computes the average deviation of MATSim results from SUMO results for test and reference. If this test fail, it means, that the average
	 * deviation increased. This test should never fail, even if setting up a new HBEFA-version! It would mean that overall results got worse.
	 * @param refComparison original reference values
	 * @param testComparison test values
	 */
	private void averageDeviation(List<WLTPLinkComparison> refComparison, List<WLTPLinkComparison> testComparison){
		// Compute the average (relative) deviation

		double[] refAverages = new double[5];
		double[] testAverages = new double[5];

		for (int i = 0; i < refComparison.size(); i++){
			refAverages[0] += refComparison.get(i).CO[3];
			refAverages[1] += refComparison.get(i).CO2[3];
			refAverages[2] += refComparison.get(i).HC[3];
			refAverages[3] += refComparison.get(i).PMx[3];
			refAverages[4] += refComparison.get(i).NOx[3];

			testAverages[0] += testComparison.get(i).CO[3];
			testAverages[1] += testComparison.get(i).CO2[3];
			testAverages[2] += testComparison.get(i).HC[3];
			testAverages[3] += testComparison.get(i).PMx[3];
			testAverages[4] += testComparison.get(i).NOx[3];
		}

		Assertions.assertTrue(testAverages[0]/refComparison.size()-1 <= refAverages[0]/refComparison.size()-1);
		Assertions.assertTrue(testAverages[1]/refComparison.size()-1 <= refAverages[1]/refComparison.size()-1);
		Assertions.assertTrue(testAverages[2]/refComparison.size()-1 <= refAverages[2]/refComparison.size()-1);
		Assertions.assertTrue(testAverages[3]/refComparison.size()-1 <= refAverages[3]/refComparison.size()-1);
		Assertions.assertTrue(testAverages[4]/refComparison.size()-1 <= refAverages[4]/refComparison.size()-1);
	}

	static Stream<Arguments> testArgs() {
		// TODO settings with args only executed for one argument (10)! It should run every argument configuration
		LinkCutSetting[] cutSettings = new LinkCutSetting[]{
			LinkCutSetting.fromLinkAttributes,
			LinkCutSetting.fixedIntervalLength.setAttr(10),
			LinkCutSetting.fixedIntervalLength.setAttr(50),
			LinkCutSetting.eachMinimum,
			LinkCutSetting.minSpacingMinimum.setAttr(10),
			LinkCutSetting.minSpacingMinimum.setAttr(50),
		};

		// All combinations
		return Stream.of("petrol", "diesel")
			.flatMap(fuel -> Arrays.stream(cutSettings)
				.map(setting -> Arguments.of(fuel, setting)));
	}

	@ParameterizedTest
	@MethodSource("testArgs")
	public void test(String fuel, LinkCutSetting cutSetting) throws IOException, URISyntaxException {
		// Prepare emission-config
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction );
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		// Create config
		Config config = ConfigUtils.createConfig(ecg);

		// Define the wltpLinkAttributes
		Path wltp_path = Paths.get(utils.getClassInputDirectory()).resolve("wltp.csv");
		List<WLTPLinkAttributes> wltpLinkAttributes = null;
		if(cutSetting == LinkCutSetting.fromLinkAttributes){
			wltpLinkAttributes = new ArrayList<>();
			wltpLinkAttributes.add(new WLTPLinkAttributes(589, 3095, 15.69, "URB/Local/50"));
			wltpLinkAttributes.add(new WLTPLinkAttributes(433, 4756, 21.28, "URB/MW-City/80"));
			wltpLinkAttributes.add(new WLTPLinkAttributes(455, 7158, 27.06, "RUR/MW/100"));
			wltpLinkAttributes.add(new WLTPLinkAttributes(323, 8254, 36.47, "RUR/MW/130"));
		} else if(cutSetting == LinkCutSetting.fixedIntervalLength){
			cutSetting.setAttr(10);
			wltpLinkAttributes = createTestLinks(wltp_path, cutSetting);
		} else if(cutSetting == LinkCutSetting.eachMinimum){
			wltpLinkAttributes = createTestLinks(wltp_path, LinkCutSetting.eachMinimum);
		} else if(cutSetting == LinkCutSetting.minSpacingMinimum){
			cutSetting.setAttr(10);
			wltpLinkAttributes = createTestLinks(wltp_path, cutSetting);
		}
		assert wltpLinkAttributes != null;

		// Read in the SUMO-outputs
		// output-files for SUMO come from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html
		Path sumo_out_path = Paths.get(utils.getClassInputDirectory()).resolve("sumo_" + fuel + "_output.csv");
		List<SumoEntry> sumoSegments = readSumoEmissionsForLinks(sumo_out_path, wltpLinkAttributes);

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
		vehicleAttributes.setHbefaSizeClass("average");
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

		// Prepare data for comparison (and print out a csv for debugging)
		List<WLTPLinkComparison> comparison = new ArrayList<>();
		int currentSecond = 0;
		for(int i = 0; i < wltpLinkAttributes.size(); i++){
			comparison.add( new WLTPLinkComparison(
				i,
				currentSecond,
				wltpLinkAttributes.get(i).time,
				wltpLinkAttributes.get(i).length,

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

				// TODO We are comparing PMx to PM10. SUMO does not specify, what PMx exactly means.
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

		// Print out the results as csv TODO Change path to output
		// NOTE: When using the outputDirectory of the MATSimTestUtils, the files will be deleted after each  run.
		// If you need the files, you have to change the path to a custom file path.
		String diff_name = "diff_" + fuel + "_" + cutSetting + "_" + cutSetting.getAttr() + "_out.csv";
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter("D:/Projects/VSP/MATSim/PHEMv2/out/" + diff_name),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"segment",
			"lengths",
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
				comparison.get(i).length,

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

		// We need to read in the reference-files with the SUMO and MATSIM results for HBEFA v4.1
		var refComparison = readReferenceComparison(Paths.get(utils.getClassInputDirectory()).resolve("diff_" + fuel + "_ref.csv"));


		// Now we have everything we need for comparing -> Compute the difference between MATSim- and SUMO-emissions

		testHbefaV4_1(refComparison, comparison);
		testValueDeviation(refComparison, comparison);
		averageDeviation(refComparison, comparison);
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
	private record SumoEntry(Type type, int second, double velocity, double acceleration, double slope, double CO, double CO2, double HC, double PMx, double NOx, double fuel, double electricity){
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
	private record WLTPLinkAttributes(int time, double length, double freespeed, String hbefaStreetType){}

	/**
	 * Helper struct containing values important for comparing SUMO and MATSim emissions.
	 * CO, CO2, HC, PM, NO are double-arrays of length 4, containing: absolute emission SUMO / absolute emission MATSim / diff / factor
	 * @param segment index of segment
	 * @param startTime in seconds
	 * @param travelTime in seconds <br>
	 */
	private record WLTPLinkComparison(int segment, int startTime, int travelTime, double length, double[] CO, double[] CO2, double[] HC, double[] PMx, double[] NOx){}

	public enum LinkCutSetting {
		/**
		 * The easiest setting: Each link corresponds to one {@link WLTPLinkAttributes}.
		 * There are 4 links in total: slow (~50km/h), medium(~80km/h), high(~100km/h), very high(~130km/h)
		 */
		fromLinkAttributes,

		/**
		 * Generates a new link at a fixed time interval.
		 */
		fixedIntervalLength {
			int interval;

			@Override
			public int getAttr() {
				return interval;
			}

			@Override
			public LinkCutSetting setAttr(int interval) {
				this.interval = interval;
				return this;
			}
		},

		/**
		 * Generates a new link at each minimum and each end of a standing period.
		 */
		eachMinimum,

		/**
		 * Generates a new link at a minimum, if the last minimum was longer ago than the minimum spacing time
		 */
		minSpacingMinimum{
			private int spacing;

			@Override
			public int getAttr() {
				return spacing;
			}

			@Override
			public LinkCutSetting setAttr(int spacing) {
				this.spacing = spacing;
				return this;
			}
		};

		public int getAttr(){
			return 0;
		};
		public LinkCutSetting setAttr(int attr){return this;}
	}
}
