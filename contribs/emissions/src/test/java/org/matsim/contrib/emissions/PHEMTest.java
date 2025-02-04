package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
	private static List<SumoEntry> readSumoEmissionsForLinks(Path path, WLTPLinkAttributes[] wltpLinkAttributes){
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
		assert sumoSegments.size() == wltpLinkAttributes.length;

		return sumoSegments;
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		// Prepare emission-config
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction ); //TODO Check that this is correct
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ); //TODO Check that this is correct
		ecg.setHbefaConsistencyChecker(EmissionsConfigGroup.UseHbefaConsistencyChecker.skip);
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		// Create config
		Config config = ConfigUtils.createConfig(ecg);

		// Define Test-case (WLTP Class 3b) -> Creates a link for each wltp-segment
		WLTPLinkAttributes[] wltpLinkAttributes = new WLTPLinkAttributes[]{
			new WLTPLinkAttributes(589, 3095, 15.69, "URB/Local/50"),
			new WLTPLinkAttributes(433, 4756, 21.28, "URB/MW-City/80"),
			new WLTPLinkAttributes(455, 7158, 27.06, "RUR/MW/100"),
			new WLTPLinkAttributes(323, 8254, 36.47, "RUR/MW/130"),
		};

		Path dir = Paths.get(utils.getClassInputDirectory()).resolve("sumo_output.csv");
		List<PHEMTest.SumoEntry> sumoSegments = readSumoEmissionsForLinks(dir, wltpLinkAttributes);

		// Define vehicle
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		vehicleAttributes.setHbefaTechnology("petrol (4S)");
		vehicleAttributes.setHbefaSizeClass("not specified"); // TODO FIx table for final tests
		vehicleAttributes.setHbefaEmConcept("PC P Euro-4");
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = new Tuple<>(
			HbefaVehicleCategory.PASSENGER_CAR,
			vehicleAttributes);

		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		// Calculate MATSim-emissions
		EmissionModule module = new EmissionModule(scenario, manager);
		List<Map<Pollutant, Double>> link_pollutant2grams = new ArrayList<>();
		for(int i = 0; i < wltpLinkAttributes.length; i++){
			link_pollutant2grams.add(module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
				wltpLinkAttributes[i].time,
				wltpLinkAttributes[i].hbefaStreetType,
				wltpLinkAttributes[i].freespeed,
				wltpLinkAttributes[i].length,
				vehHbefaInfo));
		}

		var file = Paths.get("/Users/janek/Desktop/aggregate.csv");
		try(var bufWriter = Files.newBufferedWriter(file); var csv = CSVFormat.DEFAULT.print(bufWriter)) {
			for (var i = 0; i < wltpLinkAttributes.length; i++) {
				var length_m = wltpLinkAttributes[i].length;
				var matsim_em = link_pollutant2grams.get(i);
				var sumo_em = sumoSegments.get(i).NOx();
				var matsim_em_g_km = matsim_em.get(Pollutant.NOx) / ((double) length_m / 1000);
				var sumo_em_g_km = (sumo_em/1000) / ((double) length_m / 1000);
				System.out.println("Link " + i + ": MATSim NOx=" + matsim_em_g_km + " g/km, SUMO NOx=" + sumo_em_g_km + " g/km");
			}
		}
		// Now we need to read in the sumo-files and get the SUMO results
		// output-files comes from sumo emissionsDrivingCycle: https://sumo.dlr.de/docs/Tools/Emissions.html



		// Now we have everything we need for comparing -> Compute the difference between MATSim- and SUMO-emissions and save them in a csv
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter(utils.getOutputDirectory() + "diff_out.csv"),
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

		// TODO Add max diff values
		int currentSecond = 0;
		for(int i = 0; i < wltpLinkAttributes.length; i++){
			writer.printRecord(
				i,
				currentSecond,
				wltpLinkAttributes[i].time,

				sumoSegments.get(i).CO/1000,
				link_pollutant2grams.get(i).get(Pollutant.CO),
				link_pollutant2grams.get(i).get(Pollutant.CO) - sumoSegments.get(i).CO/1000,
				computeFactor(link_pollutant2grams.get(i).get(Pollutant.CO), sumoSegments.get(i).CO/1000),

				sumoSegments.get(i).CO2/1000,
				link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL),
				link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL) - sumoSegments.get(i).CO2/1000,
				computeFactor(link_pollutant2grams.get(i).get(Pollutant.CO2_TOTAL), sumoSegments.get(i).CO2/1000),

				sumoSegments.get(i).HC/1000,
				link_pollutant2grams.get(i).get(Pollutant.HC),
				link_pollutant2grams.get(i).get(Pollutant.HC) - sumoSegments.get(i).HC/1000,
				computeFactor(link_pollutant2grams.get(i).get(Pollutant.HC), sumoSegments.get(i).HC/1000),

				sumoSegments.get(i).PMx/1000,
				link_pollutant2grams.get(i).get(Pollutant.PM),
				link_pollutant2grams.get(i).get(Pollutant.PM) - sumoSegments.get(i).PMx/1000,
				computeFactor(link_pollutant2grams.get(i).get(Pollutant.PM),sumoSegments.get(i).PMx/1000),

				sumoSegments.get(i).NOx/1000,
				link_pollutant2grams.get(i).get(Pollutant.NOx),
				link_pollutant2grams.get(i).get(Pollutant.NOx) - sumoSegments.get(i).NOx/1000,
				computeFactor(link_pollutant2grams.get(i).get(Pollutant.NOx), sumoSegments.get(i).NOx/1000)
			);
			currentSecond += wltpLinkAttributes[i].time;
		}
		writer.flush();
		writer.close();
	}

	private double computeFactor(double a, double b){
		return a/b;
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
}
