package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.ArrayMap;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.math.IEEE754rUtils.max;
import static org.apache.commons.lang3.math.IEEE754rUtils.min;

/**
 * This class contains three tests, that differ in levels of strictness: <br>
 *  (1) Values must be exact to the results of HBEFAv4.1. This test should not fail, when changes are done in the emission-contrib.
 *  	It will however fail, if a new HBEFA-version is set up. <br>
 *  (2)	Values lie between SUMO and HBEFAv4.1. This test may fail when setting up a new HBEFA-version. Affected values should be checked <br>
 *  (3) Average-deviance is better than for HBEFAv4.1. This test should never fail, even when setting up a new HBEFA-version. <br>
 *  If you need to adjust this test for future HBEFA versions, you can use the diff_{fuel}_out.csv output and change and use it as new reference file.
 *  The sumo_{fuel}_output.csv does not need to be changed, unless the PHEMLight engine got an update. TODO Update to PLv5
 */
public class PHEMTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO Files were changed to local for debugging purposes. Cange them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	// WGS83 -> SA_Lo29
	private final static GeotoolsTransformation TRANSFORMATION = new GeotoolsTransformation("EPSG:4326", "SA_Lo29");

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
	 * Reads a csv (header: time,speed,acceleration) containing a cycle and converts it into a List of {@link CycleLinkAttributes},
	 * with each element representing a section of the cycle.
	 * @param path path of the cycle csv-file
	 * @param cutSetting defining where the cuts in the cycle are set
	 */
	private static List<CycleLinkAttributes> createTestLinks(Path path, LinkCutSetting cutSetting){
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
		currentList.add(drivingCycleSeconds.getFirst());
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

		// Now build the list of Cycle Link attributes
		PHEMTestHbefaRoadTypeMapping mapping = new PHEMTestHbefaRoadTypeMapping();

		// Variables needed for the mapping
		List<CycleLinkAttributes> attributeList = new ArrayList<>();

		int i = 0;
		int finishedSegments = 0;
		for (var segment : drivingSegments) {
			i++;
			// Compute the sum of velocities of the segment (which equals the numerical integral, thus the total distance)
			// If then length of this segment is 0, then we need to append it to the next segment. A 0-segment would cause infinite emissions.
			// So we continue if len == 0.
			double len = segment.stream().map(s -> s.vel/3.6).reduce(0., Double::sum);
			if (len == 0) continue;

			int time = (segment.getLast().second+1 - drivingSegments.get(finishedSegments).getFirst().second);

			double freespeed = segment.stream().map(s -> s.vel).max(Comparator.naturalOrder()).get()/3.6;

			// Use default hbefa Hbefa Road Type mapping TODO NoOfLanes is important for this to work! Currently statically set to 1
			Link link = NetworkUtils.createLink(Id.createLinkId("l" + i), null, null, null, len, freespeed, 1, 1);
			String roadType = mapping.determineHbefaType(link);

			attributeList.add(new CycleLinkAttributes(time, len, freespeed, roadType));
			finishedSegments = i;
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
	 * Read a SUMO emissionsDrivingCycle csv-output and maps the emissions to the test links, given in {@code cycleLinkAttributes}.
	 * @param path Path to SUMO output-file
	 * @param cycleLinkAttributes Array of records, containing the information of each cycle-test-segment
	 * @return a {@link SumoEntry} for each cycle-test-segment
	 */
	private static List<SumoEntry> readSumoEmissionsForLinks(Path path, List<CycleLinkAttributes> cycleLinkAttributes){
		List<SumoEntry> sumoSeconds = readSumoFile(path);
		List<SumoEntry> sumoSegments = new ArrayList<>();

		/*
		Partition the SumoSeconds into the segments, given in cycleLinkAttributes.
		 */
		int currentSecond = 0;
		for(CycleLinkAttributes cycleLink : cycleLinkAttributes){
			int travelTime = cycleLink.time;
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
			for(; i < currentSecond + cycleLink.time; i++){
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
		assert sumoSegments.size() == cycleLinkAttributes.size();

		return sumoSegments;
	}

	private static List<CycleLinkComparison> readReferenceComparison(Path path){
		List<CycleLinkComparison> refComparison = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(",")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		try (var reader = Files.newBufferedReader(path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				refComparison.add(new CycleLinkComparison(
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

	/// Sets up the {@link EmissionsConfigGroup}
	private static Config configureTest(EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments){
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.InterpolationFraction );
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		ecg.setDuplicateSubsegments( duplicateSubsegments );
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		return ConfigUtils.createConfig(ecg);
	}

	///  Defines the links used for calculation
	private static List<CycleLinkAttributes> configureLinks(Path cyclePath, LinkCutSetting cutSetting){
		List<CycleLinkAttributes> cycleLinkAttributes = null;
		if(cutSetting == LinkCutSetting.fromLinkAttributes){
			cycleLinkAttributes = new ArrayList<>();
			cycleLinkAttributes.add(new CycleLinkAttributes(589, 3095, 15.69, "URB/Local/50"));
			cycleLinkAttributes.add(new CycleLinkAttributes(433, 4756, 21.28, "URB/MW-City/80"));
			cycleLinkAttributes.add(new CycleLinkAttributes(455, 7158, 27.06, "RUR/MW/100"));
			cycleLinkAttributes.add(new CycleLinkAttributes(323, 8254, 36.47, "RUR/MW/130"));
		} else if(cutSetting == LinkCutSetting.fixedIntervalLength){
			cycleLinkAttributes = createTestLinks(cyclePath, cutSetting);
		} else if(cutSetting == LinkCutSetting.eachMinimum){
			cycleLinkAttributes = createTestLinks(cyclePath, cutSetting);
		} else if(cutSetting == LinkCutSetting.minSpacingMinimum){
			cycleLinkAttributes = createTestLinks(cyclePath, cutSetting);
		}
		assert cycleLinkAttributes != null;

		return cycleLinkAttributes;
	}

	/// Configures the HBEFAVehicleCategory settings
	private static Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> configureVehicle(Fuel fuel) {
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		switch(fuel){
			case Fuel.petrol:
				vehicleAttributes.setHbefaTechnology("petrol (4S)");
				vehicleAttributes.setHbefaEmConcept("PC P Euro-4");
				break;
			case Fuel.diesel:
				vehicleAttributes.setHbefaTechnology("diesel");
				vehicleAttributes.setHbefaEmConcept("PC D Euro-4");
				break;
		}
		vehicleAttributes.setHbefaSizeClass("average");
		return new Tuple<>(
			HbefaVehicleCategory.PASSENGER_CAR,
			vehicleAttributes);
	}

	/**
	 * Initializes the Emission module and calculates the MATSim emissions for each link.
	 * @param config Config configured by {@link PHEMTest#configureTest}
	 * @param vehHbefaInfo Vehicle information configured by {@link PHEMTest#configureVehicle}
	 * @param cycleLinkAttributes Links built in {@link PHEMTest#configureLinks}
	 * @return A list of the results for each link.
	 */
	private static List<Map<Pollutant, Double>> calculateMATSIMEmissions(Config config,
																		 Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo,
																		 List<CycleLinkAttributes> cycleLinkAttributes){
		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		EmissionModule module = new EmissionModule(scenario, manager);
		List<Map<Pollutant, Double>> link_pollutant2grams = new ArrayList<>();
		for (CycleLinkAttributes cycleLinkAttribute : cycleLinkAttributes) {
			link_pollutant2grams.add(module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
				cycleLinkAttribute.time,
				cycleLinkAttribute.hbefaStreetType,
				cycleLinkAttribute.freespeed,
				cycleLinkAttribute.length,
				vehHbefaInfo));
		}

		return link_pollutant2grams;
	}

	/**
	 * Compares MATSim to SUMO emission data and saves the comparison values for each link.
	 * @param linkAttributes Defines the links, generated by {@link PHEMTest#configureLinks}
	 * @param link_pollutant2grams Provides the MATSim-emission data, calculated by {@link PHEMTest#calculateMATSIMEmissions}
	 * @param sumoSegments Provides the SUMO-PHEMLight-emission data, retrieved by {@link PHEMTest#readSumoEmissionsForLinks}.
	 *                     NOTE: This argument is nullable, in this case, the diff file will contain zero-entries for sumo and diff values.
	 * @return A {@link CycleLinkComparison} instance for each link containing the combined data of MATSIM and SUMO, which can be used in the subtests
	 * and {@link PHEMTest#writeDiffFile} to get a csv output-file for the test
	 */
	private static List<CycleLinkComparison> compare(List<CycleLinkAttributes> linkAttributes,
													 List<Map<Pollutant, Double>> link_pollutant2grams,
													 @Nullable List<PHEMTest.SumoEntry> sumoSegments){

		// Lambda Function which handles if a sumoSegment is a null-pointer
		BiFunction<Integer, List<SumoEntry>, PHEMTest.SumoEntry> safeGetLambda = (i, l) -> l == null ? SumoEntry.getZeroEntry() : l.get(i);

		List<CycleLinkComparison> comparison = new ArrayList<>();
		int currentSecond = 0;


		for(int i = 0; i < linkAttributes.size(); i++){
			double sumoCO = safeGetLambda.apply(i, sumoSegments).CO/1000;
			double sumoCO2 = safeGetLambda.apply(i, sumoSegments).CO2/1000;
			double sumoHC = safeGetLambda.apply(i, sumoSegments).HC/1000;
			double sumoPM = safeGetLambda.apply(i, sumoSegments).PMx/1000;
			double sumoNOx = safeGetLambda.apply(i, sumoSegments).NOx/1000;

			double matsimCO = link_pollutant2grams.get(i).get(Pollutant.CO);
			double matsimCO2 = link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL);
			double matsimHC = link_pollutant2grams.get(i).get(Pollutant.HC);
			double matsimPM = link_pollutant2grams.get(i).get(Pollutant.PM);
			double matsimNOx = link_pollutant2grams.get(i).get(Pollutant.NOx);

			comparison.add( new CycleLinkComparison(
				i,
				currentSecond,
				linkAttributes.get(i).time,
				linkAttributes.get(i).length,

				new double[]{sumoCO,
					matsimCO,
					matsimCO - sumoCO,
					sumoCO < 1e6 ? 0 : matsimCO / sumoCO},

				new double[]{sumoCO2,
					matsimCO2,
					matsimCO2 - sumoCO2,
					sumoCO2 < 1e6 ? 0 : matsimCO2 / sumoCO2},

				new double[]{sumoHC,
					matsimHC,
					matsimHC - sumoHC,
					sumoHC < 1e6 ? 0 : matsimHC / sumoHC},

				new double[]{sumoPM,
					matsimPM,
					matsimPM - sumoPM,
					sumoPM < 1e6 ? 0 : matsimPM / sumoPM},

				new double[]{sumoNOx,
					matsimNOx,
					matsimNOx - sumoNOx,
					sumoNOx < 1e6 ? 0 : matsimNOx / sumoNOx}
			));
			currentSecond += linkAttributes.get(i).time;
		}


		return comparison;
	}

	/**
	 * Print out the results as csv, useful for later analysis (via R). <br/>
	 * NOTE: When using the outputDirectory of the MATSimTestUtils, the files will be deleted after each run.
	 * If you need the files, you have to change the path to a custom file path.
	 * @param output Path and name of file: "/path/to/file.csv"
	 * @param comparison List of comparison values created by {@link PHEMTest#compare)}
	 * @throws IOException If the given output path is invalid
	 */
	private static void writeDiffFile(String output, List<CycleLinkComparison> comparison) throws IOException {
		CSVPrinter writer = new CSVPrinter(IOUtils.getBufferedWriter(output), CSVFormat.DEFAULT);
		writer.printRecord(
			"segment",
			"startTime",
			"travelTime",
			"lengths",
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

	}

	// ----- Sub-Test methods -----
	// TODO Update these tests for the paper, Add a highway deviation test

	/**
	 * Tests if the values match the HBEFA v4.1 reference values exactly.
	 * <i>NOTE: This test will most likely fail if a new HBEFA version is used. The purpose if this test is to remind you of any result-changes
	 * when switching to a new version. If the new results seem reasonable, you can override the reference file.</i>
	 */
	private void testHbefaV4_1(List<CycleLinkComparison> refComparison, List<CycleLinkComparison> testComparison) {
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
	private void testValueDeviation(List<CycleLinkComparison> refComparison, List<CycleLinkComparison> testComparison){
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
	private void averageDeviation(List<CycleLinkComparison> refComparison, List<CycleLinkComparison> testComparison){
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

	/// Returns id to the link from given pretoria network, that the GPS coordinates maps to.
	private Id<Link> mapGpsToPretoriaNetworkLink(double gpsLat, double gpsLon, Network network){
		Coord coordWGS84 = new Coord(gpsLon, gpsLat);
		Coord coordLo29 = TRANSFORMATION.transform(coordWGS84);

		// Flip XY
//		coordLo29 = new Coord(-coordLo29.getX(), -coordLo29.getY());

		return NetworkUtils.getNearestLinkExactly(network, coordLo29).getId();
	}

	/// Computes the diff time between the date and the referenceDate
	private double getCycleTimeFromDate(String date, String referenceDate) {
		try {
			Instant d1 = Instant.parse(date);
			Instant d2 = Instant.parse(referenceDate);
			return Duration.between(d2, d1).toNanos() / 1_000_000_000.0;
		} catch (Exception e1) {
			try {
				DateTimeFormatter formatter =
					DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS Z");

				ZonedDateTime d1 = ZonedDateTime.parse(date, formatter);
				ZonedDateTime d2 = ZonedDateTime.parse(referenceDate, formatter);

				return Duration.between(d2, d1).toNanos() / 1_000_000_000.0;
			} catch (Exception e2) {
				throw e2;
			}
		}
	}

	// TODO Remove for final commit, as this was just used once for data preparation
	public static void main(String[] args) {
		Network n = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_pems.xml");
		for(var link : n.getLinks().values()){
			link.getAttributes().removeAttribute("CO");
			link.getAttributes().removeAttribute("CO2_TOTAL");
			link.getAttributes().removeAttribute("CO2_pems");
			link.getAttributes().removeAttribute("CO2_rep");
			link.getAttributes().removeAttribute("CO2e");
			link.getAttributes().removeAttribute("CO_pems");
			link.getAttributes().removeAttribute("NO2");
			link.getAttributes().removeAttribute("NOx");
			link.getAttributes().removeAttribute("NOx_pems");
			link.getAttributes().removeAttribute("timeOut");
		}

		/*for(var node : n.getNodes().values()){
			node.setCoord(new Coord(-node.getCoord().getX(), -node.getCoord().getY()));
		}*/
		NetworkUtils.writeNetwork(n, "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC.xml");
	}

	// ----- Main-test method -----

	/// Start the main-test with the given settings
	public void startTest(Cycle cycle,
						  Fuel fuel,
						  LinkCutSetting cutSetting,
						  EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments,
						  boolean ignoreSumo,
						  boolean ignoreSubTests) throws IOException {
		System.out.println(fuel.toString() + cutSetting + cutSetting.getAttr());

		// Create config
		Config config = configureTest(duplicateSubsegments);

		// Define the cycleLinkAttributes
		Path cyclePath = Paths.get(utils.getClassInputDirectory()).resolve(cycle + ".csv");
		List<CycleLinkAttributes> cycleLinkAttributes = configureLinks(cyclePath, cutSetting);

		// Read in the SUMO-outputs
		// output-files for SUMO come from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html
		List<SumoEntry> sumoSegments = null;
		if (!ignoreSumo){
			Path sumo_out_path = Paths.get(utils.getClassInputDirectory()).resolve("sumo_" + cycle + "_" + fuel + "_output_pl5.csv");
			sumoSegments = readSumoEmissionsForLinks(sumo_out_path, cycleLinkAttributes);
		}

		// Define vehicle
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = configureVehicle(fuel);

		// Calculate MATSim-emissions
		List<Map<Pollutant, Double>> link_pollutant2grams = calculateMATSIMEmissions(config, vehHbefaInfo, cycleLinkAttributes);

		// Prepare data for comparison (and print out a csv for debugging)
		List<CycleLinkComparison> comparison = compare(cycleLinkAttributes, link_pollutant2grams, sumoSegments);

		// Print out the results as csv TODO Change path back to test-output folder
		String path = "/Users/aleksander/Documents/VSP/PHEMTest/diff/" + cycle + "/";
		String diff_name = "diff_" + cycle + "_" + fuel + "_output_" + duplicateSubsegments + "_" + cutSetting + "_" + cutSetting.getAttr() + ".csv";
		writeDiffFile(path + diff_name, comparison);

		// Start the tests
		if (!ignoreSubTests){
			// We need to read in the reference-files with the SUMO and MATSIM results for HBEFA v4.1
			var refComparison = readReferenceComparison(Paths.get(utils.getClassInputDirectory()).resolve("diff_" + cycle + "_" + fuel + "_ref.csv"));

			// Now we have everything we need for comparing -> Compute the difference between MATSim- and SUMO-emissions
			testHbefaV4_1(refComparison, comparison);
			testValueDeviation(refComparison, comparison);
			averageDeviation(refComparison, comparison);
		}
	}

	// ----- Caller functions -----

	@TestFactory
	Collection<DynamicTest> basicWLTPTest() {
		LinkCutSetting[] cutSettings = new LinkCutSetting[]{
			LinkCutSetting.fromLinkAttributes,
			LinkCutSetting.fixedIntervalLength.setAttr(60),
			LinkCutSetting.eachMinimum,
			LinkCutSetting.minSpacingMinimum.setAttr(30),
		};

		return Arrays.stream(Fuel.values())
			.flatMap(fuel -> Arrays.stream(cutSettings)
				.map(cutSetting -> DynamicTest.dynamicTest(
					"Inv.-WLTP-Test: Fuel=" + fuel + "; CutSetting=" + cutSetting,
					() -> startTest(
						Cycle.WLTP,
						fuel,
						cutSetting,
						EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate,
						false,
						false
					)
				))
			)
			.toList();
	}

	@TestFactory
	Collection<DynamicTest> invertedWLTPTest() {
		Cycle[] cycles = new Cycle[]{
			Cycle.WLTP,
			Cycle.WLTP_derivated_acc,
			Cycle.WLTP_inverted_time
		};

		return Arrays.stream(Fuel.values())
			.flatMap(fuel -> Arrays.stream(cycles)
				.map(cycle -> DynamicTest.dynamicTest(
					"Inv.-WLTP-Test: Fuel=" + fuel + "; Cycle=" + cycle,
					() -> startTest(
						cycle,
						fuel,
						LinkCutSetting.fixedIntervalLength.setAttr(60),
						EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate,
						true,
						true
					)
				))
			)
			.toList();
	}

	// ----- Additional/Experimental tests -----

	@ParameterizedTest
	@EnumSource(PretoriaVehicle.class)
	public void pretoriaInputsExpTest(PretoriaVehicle vehicle) throws IOException{
//		final String SVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

		// Prepare emission-config
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.InterpolationFraction );
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		ecg.setDuplicateSubsegments( EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate );
		ecg.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);
		ecg.setHandlesHighAverageSpeeds(true);
//		ecg.setAverageWarmEmissionFactorsFile(SVN + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc");
//		ecg.setAverageColdEmissionFactorsFile(SVN + "22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc");
//		ecg.setDetailedWarmEmissionFactorsFile(SVN + "944637571c833ddcf1d0dfcccb59838509f397e6.enc");
//		ecg.setDetailedColdEmissionFactorsFile(SVN + "54adsdas478ss457erhzj5415476dsrtzu.enc");
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		// Create config
		Config config = ConfigUtils.createConfig(ecg);

		// Define vehicle
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		vehicleAttributes.setHbefaTechnology(vehicle.hbefaTechnology);
		vehicleAttributes.setHbefaEmConcept(vehicle.hbefaEmConcept);
		vehicleAttributes.setHbefaSizeClass(vehicle.hbefaSizeClass);
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = new Tuple<>(
			vehicle.hbefaVehicleCategory,
			vehicleAttributes);

		System.out.println(vehHbefaInfo);

		// Read in the Pretoria network file with real emissions
		Network pretoriaNetwork = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC.xml");

		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		// Read in the csv with Pretoria GPS/PEMS data for C-route
		List<PretoriaGPSEntry> pretoriaGPSEntries = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(",")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		Path gps_path = switch (vehicle){
			case PretoriaVehicle.ETIOS -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-etios.csv");
			case PretoriaVehicle.FIGO -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-figo.csv");
		};

		try (var reader = Files.newBufferedReader(gps_path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				pretoriaGPSEntries.add(new PretoriaGPSEntry(
					record.get(0),
					Integer.parseInt(record.get(1)),
					Integer.parseInt(record.get(2)),
					Integer.parseInt(record.get(3)),
					Integer.parseInt(record.get(4)),
					Boolean.parseBoolean(record.get(5)),
					Double.parseDouble(record.get(6)),
					Double.parseDouble(record.get(7)),
					Double.parseDouble(record.get(8)),
					Double.parseDouble(record.get(9)),
					Double.parseDouble(record.get(14)),

					Double.parseDouble(record.get(39)),
					Double.parseDouble(record.get(38)),
					Double.parseDouble(record.get(40))
				));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}

		// Attach gps-information to the matsim links
		String referenceDate = pretoriaGPSEntries.getFirst().date;
		Map<Integer, Map<Id<Link>, List<PretoriaGPSEntry>>> tripId2linkId2pretoriaGpsEntries = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Tuple<Double, Double>>> tripId2linkId2enterTimeAndLeaveTimes = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Map<Pollutant, Tuple<Double, Double>>>> tripId2linkId2pollutant2emissions = new ArrayMap<>();

		// TODO Check if ArrayMaps or HashMaps are faster here
		int previousTripId = -1;
		Id<Link> previousLinkId = null;
		int i = 0;
		for(var gpsEntry : pretoriaGPSEntries){
			var tripId = gpsEntry.trip;
			tripId2linkId2pretoriaGpsEntries.putIfAbsent(tripId, new HashMap<>());
			tripId2linkId2enterTimeAndLeaveTimes.putIfAbsent(tripId, new HashMap<>());

			var linkId = mapGpsToPretoriaNetworkLink(gpsEntry.gpsLat, gpsEntry.gpsLon, pretoriaNetwork);
			tripId2linkId2pretoriaGpsEntries.get(tripId).putIfAbsent(linkId, new ArrayList<>());
			tripId2linkId2pretoriaGpsEntries.get(tripId).get(linkId).add(gpsEntry);

			// Check if a new trip has started OR if vehicle arrived at new link
			if(previousTripId != tripId || previousLinkId != linkId){
				// Duplicate link entries are not allowed. However, it can happen that the last points gets assigned to the first link. We simply skip these points.
				if(tripId2linkId2enterTimeAndLeaveTimes.get(tripId).containsKey(linkId)) {
					continue;
				}

				double currentTime = getCycleTimeFromDate(gpsEntry.date, referenceDate);

				// Start new entry
				tripId2linkId2enterTimeAndLeaveTimes.get(tripId).put(linkId, new Tuple<>(currentTime, Double.MAX_VALUE));

				// Finish old entry (except when we started a new trip)
				if (previousTripId == tripId) {
					double enterTime = tripId2linkId2enterTimeAndLeaveTimes.get(previousTripId).get(previousLinkId).getFirst();
					tripId2linkId2enterTimeAndLeaveTimes.get(previousTripId).put(previousLinkId, new Tuple<>(enterTime, currentTime));
				}
			}

			previousTripId = tripId;
			previousLinkId = linkId;
			i++;
		}

		// Add last entry TODO Check if this works properly
		double enterTime = tripId2linkId2enterTimeAndLeaveTimes.get(previousTripId).get(previousLinkId).getFirst();
		tripId2linkId2enterTimeAndLeaveTimes.get(previousTripId).put(previousLinkId, new Tuple<>(enterTime, getCycleTimeFromDate(pretoriaGPSEntries.getLast().date, referenceDate)));

		// Get the emission values (PEMS and MATSim)
		EmissionModule module = new EmissionModule(scenario, manager);

		for(var tripEntry : tripId2linkId2pretoriaGpsEntries.entrySet()){
			var tripId = tripEntry.getKey();

			for(var linkEntry : tripEntry.getValue().entrySet()){
				var linkId = linkEntry.getKey();
				var link = pretoriaNetwork.getLinks().get(linkId);

				// Extract the PEMS information
				double CO_pems = linkEntry.getValue().stream().mapToDouble(e -> e.CO).reduce(Double::sum).getAsDouble();
				double CO2_pems = linkEntry.getValue().stream().mapToDouble(e -> e.CO2).reduce(Double::sum).getAsDouble();
				double NOx_pems = linkEntry.getValue().stream().mapToDouble(e -> e.NOx).reduce(Double::sum).getAsDouble();

				// Compute the MATSim emissions
				// TODO Add cold emissions

				double time = tripId2linkId2enterTimeAndLeaveTimes.get(tripId).get(linkId).getSecond() - tripId2linkId2enterTimeAndLeaveTimes.get(tripId).get(linkId).getFirst();
				assert time > 0;

				var emissionsMatsim = module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
					time,
					(String) link.getAttributes().getAttribute("hbefa_road_type"),
					link.getFreespeed(),
					link.getLength(),
					vehHbefaInfo
				);

				double CO_matsim = emissionsMatsim.get(Pollutant.CO);
				double CO2_matsim = emissionsMatsim.get(Pollutant.CO2_TOTAL);
				double NOx_matsim = emissionsMatsim.get(Pollutant.NOx);

				tripId2linkId2pollutant2emissions.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2pollutant2emissions.get(tripId).putIfAbsent(linkId, new HashMap<>());

				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.CO, new Tuple<>(CO_matsim, CO_pems));
				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.CO2_TOTAL, new Tuple<>(CO2_matsim, CO2_pems));
				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.NOx, new Tuple<>(NOx_matsim, NOx_pems));
			}
		}

		// Save the results in a file
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/pretoria/output"),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"tripId",
			"linkId",
			"CO_MATSim",
			"CO_pems",
			"CO2_MATSim",
			"CO2_pems",
			"NOx_MATSim",
			"NOx_pems"
		);

		tripId2linkId2pollutant2emissions.forEach((tripId, linkMap) -> {
			linkMap.forEach((linkId, pollutantMap) -> {
				try {
					writer.printRecord(
						tripId,
						linkId,
						pollutantMap.get(Pollutant.CO).getFirst(),
						pollutantMap.get(Pollutant.CO).getSecond(),
						pollutantMap.get(Pollutant.CO2_TOTAL).getFirst(),
						pollutantMap.get(Pollutant.CO2_TOTAL).getSecond(),
						pollutantMap.get(Pollutant.NOx).getFirst(),
						pollutantMap.get(Pollutant.NOx).getSecond()
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		});

		writer.flush();
		writer.close();
	}


	@TestFactory
	Collection<DynamicTest> aggregatedPMExpTest() {
		return Arrays.stream(Fuel.values())
			.map(fuel -> DynamicTest.dynamicTest(
				"PM-Test: Fuel=" + fuel,
				() -> startTest(
					Cycle.CADC,
					fuel,
					LinkCutSetting.fixedIntervalLength.setAttr(60),
					EmissionsConfigGroup.DuplicateSubsegments.aggregateByFleetComposition,
					true,
					true
				)
			)).toList();
	}

	static Stream<Arguments> scaledWLTPExpProvider() {
		return Stream.of(0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5)
			.flatMap(scale ->
				Arrays.stream(Fuel.values())
					.map(fuel -> Arguments.of(scale, fuel))
			);
	}

	@ParameterizedTest
	@EnumSource(Fuel.class)
	void

	sinusCyclesExpTest(Fuel fuel) throws IOException {

		// Create config
		Config config = configureTest(EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate);

		Path cyclePath = Paths.get("/Users/aleksander/Documents/VSP/PHEMTest/style/combined_cycle.csv");

		List<CycleLinkAttributes> cycleLinkAttributes = configureLinks(cyclePath, LinkCutSetting.fixedIntervalLength.setAttr(10));

		// Read in the SUMO-outputs
		// output-files for SUMO come from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html
		List<SumoEntry> sumoSegments = null;

		// Define vehicle
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = configureVehicle(fuel);

		// Calculate MATSim-emissions
		List<Map<Pollutant, Double>> link_pollutant2grams = calculateMATSIMEmissions(config, vehHbefaInfo, cycleLinkAttributes);

		// Prepare data for comparison (and print out a csv for debugging)
		List<CycleLinkComparison> comparison = compare(cycleLinkAttributes, link_pollutant2grams, sumoSegments);

		// Print out the results as csv
		String path = "/Users/aleksander/Documents/VSP/PHEMTest/style/";
		String diff_name = "combined_matsim_" + fuel + "_output.csv";
		writeDiffFile(path + diff_name, comparison);
	}


	@ParameterizedTest
	@MethodSource("scaledWLTPExpProvider")
	void scaledWLTPExpTest(double scale, Fuel fuel) throws IOException {

		// Create config
		Config config = configureTest(EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate);

		// Define the cycleLinkAttributes
		Path cyclePath;
		if(Math.abs(scale - 1) < 1e-6){
			cyclePath = Paths.get(utils.getClassInputDirectory()).resolve("/Users/aleksander/Documents/VSP/PHEMTest/scaled/wltp1.csv");
		} else {
			cyclePath = Paths.get(utils.getClassInputDirectory()).resolve("/Users/aleksander/Documents/VSP/PHEMTest/scaled/wltp" + scale + ".csv");
		}

		List<CycleLinkAttributes> cycleLinkAttributes = configureLinks(cyclePath, LinkCutSetting.fixedIntervalLength.setAttr(60));

		// Read in the SUMO-outputs
		// output-files for SUMO come from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html
		List<SumoEntry> sumoSegments = null;

		// Define vehicle
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = configureVehicle(fuel);

		// Calculate MATSim-emissions
		List<Map<Pollutant, Double>> link_pollutant2grams = calculateMATSIMEmissions(config, vehHbefaInfo, cycleLinkAttributes);

		// Prepare data for comparison (and print out a csv for debugging)
		List<CycleLinkComparison> comparison = compare(cycleLinkAttributes, link_pollutant2grams, sumoSegments);

		// Print out the results as csv
		String path = "/Users/aleksander/Documents/VSP/PHEMTest/scaled/";
		String diff_name = Math.abs(scale - 1) < 1e-6 ? "diff_" + fuel + "1.csv" : "diff_" + fuel + "_" + scale + ".csv";
		writeDiffFile(path + diff_name, comparison);
	}

	@ParameterizedTest
	@EnumSource(Fuel.class)
	void highwayOverestimationExpTest(Fuel fuel) throws IOException {
		// Create config
		Config config = configureTest(EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate);

		// Define the cycleLinkAttributes
		Path cyclePath = Paths.get(utils.getClassInputDirectory()).resolve( "WLTP.csv");
		List<CycleLinkAttributes> cycleLinkAttributes = configureLinks(cyclePath, LinkCutSetting.fromLinkAttributes);

		// Set SUMO-ref to null
		List<SumoEntry> sumoSegments = null;

		// Define vehicle
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = configureVehicle(fuel);

		// Calculate MATSim-emissions
		List<Map<Pollutant, Double>> link_pollutant2grams = calculateMATSIMEmissions(config, vehHbefaInfo, cycleLinkAttributes);

		// Prepare data for comparison (and print out a csv for debugging)
		List<CycleLinkComparison> comparison = compare(cycleLinkAttributes, link_pollutant2grams, sumoSegments);

		// Print out the results as csvv
		String path = "/Users/aleksander/Documents/VSP/PHEMTest/diff/highwayOverestimation/";
		String diff_name = "diff_WLTP_" + fuel + "130_output.csv";
		writeDiffFile(path + diff_name, comparison);

	}

	// ----- Helper definitions -----

	private record PretoriaGPSEntry(String date, int trip, int driver, int route, int load, boolean coldStart, double gpsLat, double gpsLon, double gpsAlt, double gpsVel, double vehVel, double CO, double CO2, double NOx){};

	private record DrivingCycleSecond(int second, double vel, double acc){}

	/**
	 * Represents a row of the SUMO output-file / a segment of the cycle.
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

		static SumoEntry getZeroEntry(){
			return new SumoEntry(Type.SECOND, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
	}

	/**
	 * Represents a segment of the cycle. Each segment would be one link in MATSim.
	 * @param time travelTime [s]
	 * @param length [m]
	 * @param freespeed [m/s]
	 * @param hbefaStreetType More information at: {@link VspHbefaRoadTypeMapping}
	 */
	private record CycleLinkAttributes(int time, double length, double freespeed, String hbefaStreetType){}

	/**
	 * Helper struct containing values important for comparing SUMO and MATSim emissions.
	 * CO, CO2, HC, PM, NO are double-arrays of length 4, containing: absolute emission SUMO / absolute emission MATSim / diff / factor
	 * @param segment index of segment
	 * @param startTime in seconds
	 * @param travelTime in seconds <br>
	 */
	private record CycleLinkComparison(int segment, int startTime, int travelTime, double length, double[] CO, double[] CO2, double[] HC, double[] PMx, double[] NOx){}

	// TODO Due to enums being a singleton, the variables can cause unexpected behavior if not used properly! The current implementation propagates that the value is object bound. Change this for the final test!
	public enum LinkCutSetting {
		/**
		 * The easiest setting: Each link corresponds to predefined {@link CycleLinkAttributes}.
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
		}

		public LinkCutSetting setAttr(int attr){return this;}
	}

	public enum Fuel {
		petrol,
		diesel
	}

	public enum Cycle {
		WLTP,
		WLTP_derivated_acc,
		WLTP_inverted_time,
		CADC
	}

	public enum PretoriaVehicle{
		/// Toyota Etios 1.5 (1496ccm, 66kW) Sprint hatchback light passenger vehicle with a Euro 6 classification (138g/km) (file: public-etios.csv).
		ETIOS("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR),

		// TODO Euro 5 or Euro 6? (contradicting information)
		/// Ford Figo 1.5 (1498ccm, 91kW) Trend hatchback light passenger vehicle with a Euro 6 classification (132g/km) (file: public-figo.csv).
		FIGO("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR);

		/* TODO Add, when detailed HBEFA table for HGV available
		/// Isuzu FTR850 AMT (Road-Rail Vehicle) medium heavy vehicle with a Euro 3 classification (file: public-rrv.csv).
		RRV("diesel", "PC D Euro-3", "average", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);*/

		final String hbefaTechnology;
		final String hbefaEmConcept;
		final String hbefaSizeClass;
		final HbefaVehicleCategory hbefaVehicleCategory;

		PretoriaVehicle(String hbefaTechnology, String hbefaEmConcept, String hbefaSizeClass, HbefaVehicleCategory hbefaVehicleCategory) {
			this.hbefaTechnology = hbefaTechnology;
			this.hbefaEmConcept = hbefaEmConcept;
			this.hbefaSizeClass = hbefaSizeClass;
			this.hbefaVehicleCategory = hbefaVehicleCategory;
		}
	}
}

// TODO This class is only temporary here
class PHEMTestHbefaRoadTypeMapping extends HbefaRoadTypeMapping {

	// have this here, since the existing mappers have a build method as well.
	public static HbefaRoadTypeMapping build() {
		return new PHEMTestHbefaRoadTypeMapping();
	}

	public PHEMTestHbefaRoadTypeMapping() {
	}

	@Override
	protected String determineHbefaType(Link link) {

		var freespeed = link.getFreespeed();

		if (freespeed <= 8.333333333) { //30kmh
			return "URB/Access/30";
		} else if (freespeed <= 11.111111111) { //40kmh
			return "URB/Access/40";
		} else if (freespeed <= 13.888888889) { //50kmh
			double lanes = link.getNumberOfLanes();
			if (lanes <= 1.0) {
				return "URB/Local/50";
			} else if (lanes <= 2.0) {
				return "URB/Distr/50";
			} else if (lanes > 2.0) {
				return "URB/Trunk-City/50";
			} else {
				throw new RuntimeException("NoOfLanes not properly defined");
			}
		} else if (freespeed <= 16.666666667) { //60kmh
			double lanes = link.getNumberOfLanes();
			if (lanes <= 1.0) {
				return "URB/Local/60";
			} else if (lanes <= 2.0) {
				return "URB/Trunk-City/60";
			} else if (lanes > 2.0) {
				return "URB/MW-City/60";
			} else {
				throw new RuntimeException("NoOfLanes not properly defined");
			}
		} else if (freespeed <= 19.444444444) { //70kmh
			return "URB/MW-City/70";
		} else if (freespeed <= 22.222222222) { //80kmh
			return "URB/MW-Nat./80";
		} else if (freespeed <= 25) {// 90kmh
			return "RUR/MW/90";
		} else if (freespeed <= 27.77777777){ // 100kmh
			return "RUR/MW/100";
		} else if (freespeed <= 30.55555555){ // 110kmh
			return "RUR/MW/110";
		} else if (freespeed <= 33.33333333){ // 120kmh
			return "RUR/MW/120";
		} else if (freespeed <= 36.11111111){ // 130kmh
			return "RUR/MW/130";
		} else if (freespeed > 36.11111111) { //faster
			return "RUR/MW/>130";
		} else {
			throw new RuntimeException("No mapping specified for links with freespeed: " + link.getFreespeed() + " and " + link.getNumberOfLanes() + " lanes");
		}
	}

}
