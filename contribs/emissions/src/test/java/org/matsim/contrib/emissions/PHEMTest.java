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
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PHEMTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	//TODO Check if the used hbefa files are correct
	private final static String HBEFA_4_1_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private final static String HBEFA_HOT = HBEFA_4_1_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
	private final static String HBEFA_COLD = HBEFA_4_1_PATH + "22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc";

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

	@Test
	public void test() throws IOException, URISyntaxException {
		// Prepare emission-config
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction ); //TODO Check that this is correct
		ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable ); //TODO Check that this is correct
		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD);

		// Create config
		Config config = ConfigUtils.createConfig(ecg);

		// Define Test-case (WLTP Class 3b) -> Creates a link for each wltp-segment
		WLTPLinkAttributes[] wltpLinkAttributes = new WLTPLinkAttributes[]{
			new WLTPLinkAttributes(589, 3095, 15.69, "URB/Local/50"),
			new WLTPLinkAttributes(433, 4756, 21.28, "URB/MW-City/80"),
			new WLTPLinkAttributes(455, 7158, 27.06, "RUR/MW/100"),
			new WLTPLinkAttributes(323, 8254, 36.47, "RUR/MW/>130"),
		};

		// Define vehicle
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehHbefaInfo = new Tuple<>(
			HbefaVehicleCategory.PASSENGER_CAR,
			new HbefaVehicleAttributes()); // TODO: Input the actual vehicle data here, currently just "average"

		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		// Calculate MATSim-emissions
		EmissionModule module = new EmissionModule(scenario, manager);
		List<Map<Pollutant, Double>> link_pollutant2grams = new ArrayList<>();
		for(int i = 0; i < 4; i++){
			link_pollutant2grams.add(module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
				wltpLinkAttributes[i].time,
				wltpLinkAttributes[i].hbefaStreetType,
				wltpLinkAttributes[i].freespeed,
				wltpLinkAttributes[i].length,
				vehHbefaInfo));
		}

		// No we need to read in the sumo-files

		System.out.println();
	}

	private record DrivingCycleSecond(int second, double vel, double acc){}

	private record WLTPLinkAttributes(int time, int length, double freespeed, String hbefaStreetType){}
}
