package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.matsim.core.utils.collections.ArrayMap;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class PretoriaTest {
	// TODO Replace paths with Test-paths

	private static final Logger logger = LogManager.getLogger(PretoriaTest.class);

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private final static String SVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

	// TODO Files were changed to local for debugging purposes. Change them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Concept_Aleks_Average_V1.1.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Concept_Aleks_V1.1.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	// TODO Remove for final commit, as this was just used once for data preparation
	public static void main(String[] args) {
		Network cRoute = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_pems.xml");
		Network full = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_hbefa.xml.gz");

		for(var link : cRoute.getLinks().values()){
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

		// Fix the wrong route
		cRoute.removeLink(Id.createLinkId("burnett1"));
		cRoute.removeLink(Id.createLinkId("33150"));
		cRoute.removeLink(Id.createLinkId("9159"));
		cRoute.removeLink(Id.createLinkId("45106"));
		cRoute.removeLink(Id.createLinkId("45107"));
		cRoute.removeLink(Id.createLinkId("9148"));
		cRoute.removeLink(Id.createLinkId("9144"));
		cRoute.removeLink(Id.createLinkId("49854"));
		cRoute.removeLink(Id.createLinkId("52267"));
		cRoute.removeLink(Id.createLinkId("46065"));

		// Section 1

		cRoute.addNode(full.getNodes().get(Id.createNodeId("25395953")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("25395954")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("25395955")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("282871644")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5837011387")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5846507042")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5846507043")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5846507044")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5846507045")));

		cRoute.addLink(full.getLinks().get(Id.createLinkId("36669")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("36680")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("36674")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("36671")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("36676")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("36678")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("42188")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("46067")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("46070")));

		// Section 2

		cRoute.addNode(full.getNodes().get(Id.createNodeId("25290978")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("25291065")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("5838748611")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("799905988")));
		cRoute.addNode(full.getNodes().get(Id.createNodeId("799906053")));

		cRoute.addLink(full.getLinks().get(Id.createLinkId("38199")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("38404")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("42127")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("6010")));
		cRoute.addLink(full.getLinks().get(Id.createLinkId("7080")));

		// Missing link

		NetworkUtils.createAndAddLink(
			cRoute,
			Id.createLinkId("cRouteLink"),
			cRoute.getNodes().get(Id.createNodeId("25290978")),
			cRoute.getNodes().get(Id.createNodeId("25395955")),
			490,
			50./3.,
			3000,
			3
		);

		cRoute.getLinks().get(Id.createLinkId("cRouteLink")).getAttributes().putAttribute("hbefa_road_type", "URB/Distr/60");

		// Add height information (no fancy algorithm, just take the elevation of the nearest GPS point)
		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(",")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		List<Tuple<Coord, Double>> coordHeights = new ArrayList<>();
		try (var reader = Files.newBufferedReader(Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-ETIOS.csv")); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				coordHeights.add(
					new Tuple<> (
						convertWGS84toLo29(Double.parseDouble(record.get("gps_lat")), Double.parseDouble(record.get("gps_lon"))),
						Double.parseDouble(record.get("gps_alt"))
					)
				);
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}

		// TODO Some parts of the coord mapping assume 2d coords, maybe add as attribute instead
		for(var node : cRoute.getNodes().values()){
			double alt = coordHeights.parallelStream()
				.map(t -> new Tuple<>(CoordUtils.calcEuclideanDistance(node.getCoord(), t.getFirst()), t.getSecond()))
				.min(Comparator.comparingDouble(Tuple::getFirst))
				.map(Tuple::getSecond)
				.orElseThrow();

//			node.setCoord(new Coord(node.getCoord().getX(), node.getCoord().getY(), alt));
			node.getAttributes().putAttribute("alt", alt);
		}

		/*for(var node : cRoute.getNodes().values()){
			node.setCoord(new Coord(-node.getCoord().getX(), -node.getCoord().getY()));
		}*/

		NetworkUtils.writeNetwork(cRoute, "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_etios.xml");
	}

	// WGS83 -> SA_Lo29
	private final static GeotoolsTransformation TRANSFORMATION = new GeotoolsTransformation("EPSG:4326", "SA_Lo29");

	/// Returns coord in SA_Lo29 format, that the GPS coordinates maps to.
	private static Coord convertWGS84toLo29(double gpsLat, double gpsLon){
		Coord coordWGS84 = new Coord(gpsLon, gpsLat);
		return TRANSFORMATION.transform(coordWGS84);
	}

	private static EmissionsConfigGroup getEmissionsConfigGroup(PretoriaVehicle vehicle, EmissionsConfigGroup.EmissionsComputationMethod method) {
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( method );
		switch (vehicle){
			case FIGO, ETIOS, RRV -> ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			case FIGO_TECHAVG, RRV_TECHAVG -> ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort );
		}
		ecg.setDuplicateSubsegments( EmissionsConfigGroup.DuplicateSubsegments.useFirstDuplicate );
		ecg.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);
		ecg.setHandlesHighAverageSpeeds(true);
//		ecg.setAverageWarmEmissionFactorsFile(SVN + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc");
//		ecg.setAverageColdEmissionFactorsFile(SVN + "22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc");
//		ecg.setDetailedWarmEmissionFactorsFile(SVN + "944637571c833ddcf1d0dfcccb59838509f397e6.enc");
//		ecg.setDetailedColdEmissionFactorsFile(SVN + "54adsdas478ss457erhzj5415476dsrtzu.enc");

		ecg.setAverageWarmEmissionFactorsFile(HBEFA_HOT_AVG);
		ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
		ecg.setAverageColdEmissionFactorsFile(HBEFA_COLD_AVG);
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		return ecg;
	}

	private Map<Integer, List<PretoriaGPSEntry>> readGpsEntries(PretoriaVehicle vehicle){
		// Read in the csv with Pretoria GPS/PEMS data for C-route
		Map<Integer, List<PretoriaGPSEntry>> tripId2pretoriaGPSEntries = new HashMap<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(",")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		Path gps_path = switch (vehicle){
			case ETIOS -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-etios.csv");
			case FIGO, FIGO_TECHAVG -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-figo.csv");
			case RRV, RRV_TECHAVG -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-rrv.csv");
		};

		String referenceDate = switch(vehicle){
			case ETIOS -> "2022-11-15T07:34:18.115Z";
			case FIGO, FIGO_TECHAVG -> "07/27/2021 10:03:03.131 +0200";
			case RRV, RRV_TECHAVG -> "02/02/2021 10:09:42.773 +0200";
		};

		try (var reader = Files.newBufferedReader(gps_path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				var tripId = Integer.parseInt(record.get("trip"));
				var CO = Double.parseDouble(record.get("CO_mass"));
				var CO2 = Double.parseDouble(record.get("CO2_mass"));
				var NOx = Double.parseDouble(record.get("NOx_mass"));

				tripId2pretoriaGPSEntries.putIfAbsent(tripId, new ArrayList<>());
				tripId2pretoriaGPSEntries.get(tripId).add(new PretoriaGPSEntry(
					getCycleTimeFromDate(record.get("date"), referenceDate),
					tripId,
					Integer.parseInt(record.get("driver")),
					Integer.parseInt(record.get("route")),
					Integer.parseInt(record.get("load")),
					Boolean.parseBoolean(record.get("coldStart")),
					convertWGS84toLo29(Double.parseDouble(record.get("gps_lat")), Double.parseDouble(record.get("gps_lon"))),
					Double.parseDouble(record.get("gps_alt")),
					Double.parseDouble(record.get("gps_speed")),
					Double.parseDouble(record.get("speed_vehicle")),

					CO < 0 ? 0 : CO,
					CO2 < 0 ? 0 : CO2,
					NOx < 0 ? 0 : NOx
				));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}

		return tripId2pretoriaGPSEntries;
	}

	private List<PretoriaGPSEntry> filterUnwantedLinkEntries(List<PretoriaGPSEntry> gpsEntries, Set<Id<Link>> unwantedLinks, Network network){
		List<PretoriaGPSEntry> filteredGpsEntries = new ArrayList<>(gpsEntries.size());

		for(var e : gpsEntries){
			Link l = NetworkUtils.getNearestLinkExactly(network, e.coord);
			if(!unwantedLinks.contains(l.getId()))
				filteredGpsEntries.add(e);
		}

		return filteredGpsEntries;
	}

	/// Removes clusters, that happen due to standing times
	private List<PretoriaGPSEntry> clusterStandingGpsEntries(List<PretoriaGPSEntry> gpsEntries){
		List<PretoriaGPSEntry> cleanedEntries = new ArrayList<>(gpsEntries);

		int i = 0;
		while(i < cleanedEntries.size()-1){
			PretoriaGPSEntry e = cleanedEntries.get(i);

			// Check if veh speed is less than 1 km/h
			if (e.vehVel < 1){
				double CO = cleanedEntries.get(i).CO;
				double CO2 = cleanedEntries.get(i).CO2;
				double NOx = cleanedEntries.get(i).NOx;

				// Create updated entry i+1
				var updatedEntry = new PretoriaGPSEntry(
					cleanedEntries.get(i+1).time,
					cleanedEntries.get(i+1).trip,
					cleanedEntries.get(i+1).driver,
					cleanedEntries.get(i+1).route,
					cleanedEntries.get(i+1).load,
					cleanedEntries.get(i+1).coldStart,
					cleanedEntries.get(i+1).coord,
					cleanedEntries.get(i+1).gpsAlt,
					cleanedEntries.get(i+1).gpsVel,
					cleanedEntries.get(i+1).vehVel,
					cleanedEntries.get(i+1).CO + CO,
					cleanedEntries.get(i+1).CO2 + CO2,
					cleanedEntries.get(i+1).NOx + NOx
				);

				// Update the list
				cleanedEntries.set(i+1, updatedEntry);
				cleanedEntries.remove(i);
			} else {
				i++;
			}
		}

		return cleanedEntries;
	}

	/// Computes the diff time between the date and the referenceDate
	private double getCycleTimeFromDate(String date, String referenceDate) {
		try {
			Instant d1 = Instant.parse(date);
			Instant d2 = Instant.parse(referenceDate);
			return Duration.between(d2, d1).toNanos() / 1_000_000_000.0;
		} catch (Exception e1) {
			DateTimeFormatter formatter =
				DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS Z");

			ZonedDateTime d1 = ZonedDateTime.parse(date, formatter);
			ZonedDateTime d2 = ZonedDateTime.parse(referenceDate, formatter);

			return Duration.between(d2, d1).toNanos() / 1_000_000_000.0;
		}
	}

	public static Stream<Arguments> pretoriaInputsExpTestParams(){
		var methods = List.of(
			EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction,
			EmissionsConfigGroup.EmissionsComputationMethod.InterpolationFraction,
			EmissionsConfigGroup.EmissionsComputationMethod.BilinearInterpolationFraction
		);

		return Stream.of(PretoriaVehicle.values())
			.flatMap(v -> methods.stream().map(m -> Arguments.of(v, m)));
	}

	@BeforeAll
	public static void printNetworkInformation() throws IOException, RuntimeException {
		// Link information used for output analysis

		// Read in the Pretoria network file with real emissions
		Network pretoriaNetwork = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_etios.xml");

		// Save the results in a file
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/pretoria/networkInformation.csv"),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"linkId",
			"freespeed",
			"roadType",
			"gradient"
		);

		for (Link l : pretoriaNetwork.getLinks().values()) {
			double dAlt = (Double) l.getToNode().getAttributes().getAttribute("alt") - (Double) l.getFromNode().getAttributes().getAttribute("alt");
			writer.printRecord(l.getId(), l.getFreespeed(), l.getAttributes().getAttribute("hbefa_road_type"), dAlt / l.getLength());
		}

		writer.flush();
		writer.close();
	}

	@ParameterizedTest
	@MethodSource("pretoriaInputsExpTestParams")
	public void pretoriaInputsExpTest(PretoriaVehicle vehicle, EmissionsConfigGroup.EmissionsComputationMethod method) throws IOException {
		// Prepare config
		EmissionsConfigGroup ecg = getEmissionsConfigGroup(vehicle, method);
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
		Network pretoriaNetwork = NetworkUtils.readNetwork("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_etios.xml");

		// Create Scenario and EventManager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager manager = EventsUtils.createEventsManager(config);

		// Read in the csv with Pretoria GPS/PEMS data for C-route
		Set<Id<Link>> unwantedLinks = Set.of(Id.createLinkId("6555"));

		Map<Integer, List<PretoriaGPSEntry>> tripId2pretoriaGpsEntries = readGpsEntries(vehicle);
//		Map<Integer, Map<PretoriaGPSEntry, Link>> tripId2gpsEntry2link = new ArrayMap<>();
//		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2gpsEntry2link.put(tripId, mapEntriesToNetwork(entries, pretoriaNetwork)));

		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, filterUnwantedLinkEntries(entries, unwantedLinks, pretoriaNetwork)));
		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, clusterStandingGpsEntries(entries)));

		// Map GPS Information to Network Links
		EmissionModule module = new EmissionModule(scenario, manager);

		Map<Integer, Integer> tripId2load = new ArrayMap<>();
		Map<Integer, Integer> tripId2driver = new ArrayMap<>();
		Map<Integer, Boolean> tripId2coldStart = new ArrayMap<>();

		Map<Integer, Map<Id<Link>, Map<Pollutant, Double>>> tripId2linkId2pollutant2realEmission = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Map<Pollutant, Double>>> tripId2linkId2pollutant2warmEmission = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Double>> tripId2linkId2pollutant2averageVelocity = new ArrayMap<>();
		Map<Integer, Map<Pollutant, Double>> tripId2pollutant2coldEmission = new ArrayMap<>();

		tripId2pretoriaGpsEntries.forEach((tripId, gpsEntries) -> {

			tripId2load.putIfAbsent(tripId, gpsEntries.getFirst().load);
			tripId2driver.putIfAbsent(tripId, gpsEntries.getFirst().driver);
			tripId2coldStart.putIfAbsent(tripId, gpsEntries.getFirst().coldStart);

			// Calculate cold Emissions (but not for RRV, as RRV has no available HBEFA Cold Table)
			if(vehicle.computeColdEmissions){
				vehHbefaInfo.getSecond().setHbefaEmConcept("average");
				var coldEmissionsMatsim = module.getColdEmissionAnalysisModule().calculateColdEmissions(
					Id.createVehicleId("0"),
					13*3600,
					vehHbefaInfo,
					2
				);
				vehHbefaInfo.getSecond().setHbefaEmConcept(vehicle.hbefaEmConcept);

				tripId2pollutant2coldEmission.putIfAbsent(tripId, new HashMap<>());
				tripId2pollutant2coldEmission.get(tripId).put(Pollutant.CO, coldEmissionsMatsim.get(Pollutant.CO));
				tripId2pollutant2coldEmission.get(tripId).put(Pollutant.CO2_TOTAL, coldEmissionsMatsim.get(Pollutant.CO2_TOTAL));
				tripId2pollutant2coldEmission.get(tripId).put(Pollutant.NOx, coldEmissionsMatsim.get(Pollutant.NOx));
			}

			GPSToNetworkMapping1D mapping1D = new GPSToNetworkMapping1D(pretoriaNetwork, gpsEntries);
			mapping1D.mapping();
			mapping1D.computeLinkShares();

			// Extract PEMS Emissions
			mapping1D.gpsEntry2linkId2proportion.forEach((gpsEntry, linkId2proportion) ->
				linkId2proportion.forEach((linkId, proportion) -> {
				double CO_pems = proportion*gpsEntry.CO;
				double CO2_pems = proportion*gpsEntry.CO2;
				double NOx_pems = proportion*gpsEntry.NOx;

				tripId2linkId2pollutant2realEmission.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2pollutant2realEmission.get(tripId).putIfAbsent(linkId, new HashMap<>());
				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).putIfAbsent(Pollutant.CO, 0.);
				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).putIfAbsent(Pollutant.CO2_TOTAL, 0.);
				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).putIfAbsent(Pollutant.NOx, 0.);

				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).put(Pollutant.CO,
					tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).get(Pollutant.CO) + CO_pems);
				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).put(Pollutant.CO2_TOTAL,
					tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).get(Pollutant.CO2_TOTAL) + CO2_pems);
				tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).put(Pollutant.NOx,
					tripId2linkId2pollutant2realEmission.get(tripId).get(linkId).get(Pollutant.NOx) + NOx_pems);

			}));

			// Compute MATSim Emissions
			mapping1D.linkId2time.forEach((linkId, time) -> {
				var link = pretoriaNetwork.getLinks().get(linkId);
				double alt = (Double) link.getToNode().getAttributes().getAttribute("alt") - (Double) link.getFromNode().getAttributes().getAttribute("alt");

				var warmEmissionsMatsim = module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
					time,
					(String) link.getAttributes().getAttribute("hbefa_road_type"),
					link.getFreespeed(),
					link.getLength(),
					alt,
					vehHbefaInfo
				);

				tripId2linkId2pollutant2warmEmission.putIfAbsent(tripId, new HashMap<>());

				tripId2linkId2pollutant2warmEmission.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2pollutant2warmEmission.get(tripId).putIfAbsent(linkId, new HashMap<>());
				tripId2linkId2pollutant2warmEmission.get(tripId).get(linkId).put(Pollutant.CO, warmEmissionsMatsim.get(Pollutant.CO));
				tripId2linkId2pollutant2warmEmission.get(tripId).get(linkId).put(Pollutant.CO2_TOTAL, warmEmissionsMatsim.get(Pollutant.CO2_TOTAL));
				tripId2linkId2pollutant2warmEmission.get(tripId).get(linkId).put(Pollutant.NOx, warmEmissionsMatsim.get(Pollutant.NOx));

				tripId2linkId2pollutant2averageVelocity.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2pollutant2averageVelocity.get(tripId).put(linkId, link.getLength()/time);
			});

		});

		// Save the results in a file
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/pretoria/output_" + vehicle + "_" + method + ".csv"),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"tripId",
			"linkId",
			"load",
			"driver",
			"segment",
			"CO_MATSim",
			"CO_pems",
			"CO2_MATSim",
			"CO2_pems",
			"NOx_MATSim",
			"NOx_pems",
			"averageVelocity"
		);

		AtomicReference<String> segment = new AtomicReference<>("none");
		tripId2linkId2pollutant2realEmission.forEach((tripId, linkId2pollutant2realEmissions) -> {
			var linkId2pollutant2warmEmissions = tripId2linkId2pollutant2warmEmission.get(tripId);

			// Print the cold emissions if trip had a cold start
			if(tripId2coldStart.get(tripId) && vehicle.computeColdEmissions){
				var coldPollutantMap = tripId2pollutant2coldEmission.get(tripId);
				try {
					writer.printRecord(
						tripId,
						"cold",
						tripId2load.get(tripId),
						tripId2driver.get(tripId),
						"none",
						coldPollutantMap.get(Pollutant.CO),
						coldPollutantMap.get(Pollutant.CO),
						coldPollutantMap.get(Pollutant.CO2_TOTAL),
						coldPollutantMap.get(Pollutant.CO2_TOTAL),
						coldPollutantMap.get(Pollutant.NOx),
						coldPollutantMap.get(Pollutant.NOx)
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			linkId2pollutant2realEmissions.forEach((linkId, pollutant2realEmissions) -> {
				var pollutant2warmEmissions = linkId2pollutant2warmEmissions.get(linkId);

				if(linkId.equals(Id.createLinkId("28948")))
					segment.set("A");
				if(linkId.equals(Id.createLinkId("14100")))
					segment.set("none");

				if(linkId.equals(Id.createLinkId("11614")))
					segment.set("B");
				if(linkId.equals(Id.createLinkId("28906")))
					segment.set("none");

				if(linkId.equals(Id.createLinkId("waterkloof4_waterkloof5")))
					segment.set("C");
				if(linkId.equals(Id.createLinkId("37156")))
					segment.set("none");

				try {
					writer.printRecord(
						tripId,
						linkId,
						tripId2load.get(tripId),
						tripId2driver.get(tripId),
						segment,
						pollutant2warmEmissions.get(Pollutant.CO),
						pollutant2realEmissions.get(Pollutant.CO),
						pollutant2warmEmissions.get(Pollutant.CO2_TOTAL),
						pollutant2realEmissions.get(Pollutant.CO2_TOTAL),
						pollutant2warmEmissions.get(Pollutant.NOx),
						pollutant2realEmissions.get(Pollutant.NOx),
						tripId2linkId2pollutant2averageVelocity.get(tripId).get(linkId)
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		});
		writer.flush();
		writer.close();

	}

	// ----- Helper definitions -----

	private record PretoriaGPSEntry(double time, int trip, int driver, int route, int load, boolean coldStart, Coord coord, double gpsAlt, double gpsVel, double vehVel, double CO, double CO2, double NOx){}

	public enum PretoriaVehicle{
		/// Ford Figo 1.5 (1498ccm, 91kW) Trend hatchback light passenger vehicle with a Euro 6 classification (132g/km) (file: public-figo.csv).
		FIGO("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR, true),
		FIGO_TECHAVG("petrol (4S)", "average", "average", HbefaVehicleCategory.PASSENGER_CAR, true),

		/// Toyota Etios 1.5 (1496ccm, 66kW) Sprint hatchback light passenger vehicle with a Euro 6 classification (138g/km) (file: public-etios.csv).
		ETIOS("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR, true),

		//TODO Add load entry
		/// Isuzu FTR850 AMT (Road-Rail Vehicle) medium heavy vehicle with a Euro 3 classification (file: public-rrv.csv).
		RRV("diesel", "HGV D Euro-III", "average", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE, false),
		RRV_TECHAVG("diesel", "average", "average", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE, false),
//		RRV("diesel", "HGV D Euro-III", "RT >7.5-12t", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE),
		;

		final String hbefaTechnology;
		final String hbefaEmConcept;
		final String hbefaSizeClass;
		final HbefaVehicleCategory hbefaVehicleCategory;
		final boolean computeColdEmissions;

		PretoriaVehicle(String hbefaTechnology, String hbefaEmConcept, String hbefaSizeClass, HbefaVehicleCategory hbefaVehicleCategory, boolean computeColdEmissions) {
			this.hbefaTechnology = hbefaTechnology;
			this.hbefaEmConcept = hbefaEmConcept;
			this.hbefaSizeClass = hbefaSizeClass;
			this.hbefaVehicleCategory = hbefaVehicleCategory;
			this.computeColdEmissions = computeColdEmissions;
		}
	}

	// TODO Very slow, optimize
	private static class GPSToNetworkMapping1D{
		// Needed for computation
		Network route;
		List<PretoriaGPSEntry> gpsEntries;

		// Prepared by class
		List<Link> linkOrder;
		List<Double> accumulatedLinkLengths4links;
		List<Double> projectedAccumulatedDistances4gpsEntries;

		//Outputs
		List<Double> accumulatedDistance4gpsEntries;
		Map<PretoriaGPSEntry, Map<Id<Link>, Double>> gpsEntry2linkId2proportion;
		Map<Id<Link>, Double> linkId2time;

		public GPSToNetworkMapping1D(Network route, List<PretoriaGPSEntry> gpsEntries) {
			this.route = route;
			this.gpsEntries = gpsEntries;

			this.linkOrder = getLinkOrder();
			this.accumulatedLinkLengths4links = getAccumulatedLinkLengths();
			this.projectedAccumulatedDistances4gpsEntries = getProjectedAccumulatedDistances();
		}

		/// Returns the path links from start to end using BFS. TODO Check/Test
		private List<Link> retrieveInBetweenLinks(Link startLink, Link endLink){
			if(startLink.getId().equals(endLink.getId()))
				return new ArrayList<>(List.of(startLink));

			Map<Link, Link> parent = new HashMap<>();
			Queue<Link> queue = new ArrayDeque<>();
			Set<Link> visited = new HashSet<>();

			queue.add(startLink);
			visited.add(startLink);

			while (!queue.isEmpty()) {
				Link current = queue.poll();

				if (current.equals(endLink)) {
					break;
				}

				NetworkUtils.getIncidentLinks(current).forEach((id, next) -> {
					if (!visited.contains(next)) {
						visited.add(next);
						parent.put(next, current);
						queue.add(next);
					}
				});
			}

			if (!parent.containsKey(endLink)) {
				throw new RuntimeException("No path found");
			}

			List<Link> path = new LinkedList<>();
			for (Link cur = endLink; cur != null; cur = parent.get(cur)) {
				path.addFirst(cur);
			}

			return path;
		}

		private List<Link> getLinkOrder(){
			List<Link> linkOrder = new ArrayList<>(gpsEntries.size());

			for(var e : gpsEntries){
				Link nearestLink = NetworkUtils.getNearestLinkExactly(route, e.coord);
				if(linkOrder.isEmpty() || !linkOrder.getLast().equals(nearestLink)){
					List<Link> inBetweenLinks = linkOrder.isEmpty() ? List.of(nearestLink, nearestLink) : retrieveInBetweenLinks(linkOrder.getLast(), nearestLink);
					linkOrder.addAll(inBetweenLinks.subList(1, inBetweenLinks.size()));
				}
			}

			return linkOrder;
		}

		private List<Double> getAccumulatedLinkLengths() {
			List<Double> accumulatedLinkLengths = new ArrayList<>(linkOrder.size());

			for(var l : linkOrder){
				accumulatedLinkLengths.add(accumulatedLinkLengths.isEmpty() ? l.getLength() : accumulatedLinkLengths.getLast() + l.getLength());
			}

			return accumulatedLinkLengths;
		}

		private Node getSharedNode(Link a, Link b){
			// Get the node, that is shared by both links
			Node sharedNode;
			if (a.getFromNode().equals(b.getFromNode()) || a.getFromNode().equals(b.getToNode())) {
				sharedNode = a.getFromNode();
			} else if (a.getToNode().equals(b.getFromNode()) || a.getToNode().equals(b.getToNode())) {
				sharedNode = a.getToNode();
			} else {
				logger.error("The given links ({}, {}) are not incident!", a.getId(), b.getId());
				return null;
			}
			return sharedNode;
		}

		private List<Double> getProjectedAccumulatedDistances(){
			List<Double> projectedAccumulatedDistances = new ArrayList<>(gpsEntries.size());

			for (PretoriaGPSEntry e : gpsEntries) {
				Link l = NetworkUtils.getNearestLinkExactly(route, e.coord);
				int index = linkOrder.indexOf(l);
				double prevLen = index == 0 ? 0 : accumulatedLinkLengths4links.get(index-1);

				Coord p = CoordUtils.orthogonalProjectionOnLineSegment(l.getFromNode().getCoord(), l.getToNode().getCoord(), e.coord);
				Node sharedNode = index == 0 ? l.getFromNode() : getSharedNode(l, linkOrder.get(index-1));
				assert sharedNode != null;
				double projectedLen = CoordUtils.calcEuclideanDistance(sharedNode.getCoord(), p);

				projectedAccumulatedDistances.add(prevLen + projectedLen);
			}
			return projectedAccumulatedDistances;
		}

		private void mapping(){
			accumulatedDistance4gpsEntries = new ArrayList<>(gpsEntries.size());
			accumulatedDistance4gpsEntries.add(0.);

			for (int i = 1; i < gpsEntries.size(); i++){
				double naiveLen = CoordUtils.calcEuclideanDistance(gpsEntries.get(i).coord, gpsEntries.get(i-1).coord);
				accumulatedDistance4gpsEntries.add(accumulatedDistance4gpsEntries.getLast() + naiveLen);
			}
		}

		private int getNextLargerIndex(List<Double> list, double dist){
			int index = Collections.binarySearch(list, dist);
			if(index >= 0)
				return index;
			else
				return -index - 1;
		}

		// TODO Velocities still overestimated, check why
		private void computeLinkShares() {
			gpsEntry2linkId2proportion = new ArrayMap<>();
			linkId2time = new ArrayMap<>();

			var accDistances = accumulatedDistance4gpsEntries;
			var accLen = accDistances.getLast();

			// Lengths can be different, adjust accumulated lengths to MATSim lengths
			System.out.println("#1234:" + (accumulatedLinkLengths4links.getLast()/accLen));
			accDistances = accDistances.stream().map(d -> d*(1-1e-6)*(accumulatedLinkLengths4links.getLast()/accLen)).toList();

			for (int i = 1; i < gpsEntries.size(); i++) {
				var dt = gpsEntries.get(i).time - gpsEntries.get(i - 1).time;

				var startAccDist = accDistances.get(i - 1);
				var endAccDist = accDistances.get(i);

				var startIndex = getNextLargerIndex(accumulatedLinkLengths4links, startAccDist);
				var endIndex = getNextLargerIndex(accumulatedLinkLengths4links, endAccDist);

				if (startIndex == endIndex) {
					// Both entries map to the same link, so add the whole time to the link
					var linkId = linkOrder.get(startIndex).getId();

					gpsEntry2linkId2proportion.putIfAbsent(gpsEntries.get(i), new ArrayMap<>());
					gpsEntry2linkId2proportion.get(gpsEntries.get(i)).put(linkId, 1.);

					linkId2time.putIfAbsent(linkId, 0.);
					linkId2time.put(linkId, linkId2time.get(linkId) + dt);
					continue;
				}

				// We are switching links, distribute the time linearly along the links
				List<Link> path = linkOrder.subList(startIndex, endIndex+1);

				var dD = endAccDist - startAccDist;

				var segmentShares = 0.;
				for (int j = 0; j < path.size(); j++) {
					var linkId = path.get(j).getId();

					double segmentLength;
					if (j == 0) {
						segmentLength = accumulatedLinkLengths4links.get(startIndex) - startAccDist;
					} else if (j == path.size() - 1) {
						segmentLength = endAccDist - accumulatedLinkLengths4links.get(endIndex - 1);
					} else {
						segmentLength = path.get(j).getLength();
					}

					gpsEntry2linkId2proportion.putIfAbsent(gpsEntries.get(i), new ArrayMap<>());
					gpsEntry2linkId2proportion.get(gpsEntries.get(i)).put(linkId, segmentLength / dD);

					linkId2time.putIfAbsent(linkId, 0.);
					linkId2time.put(linkId, linkId2time.get(linkId) + dt * segmentLength / dD);

					segmentShares += segmentLength / dD;
				}
				assert Math.abs(segmentShares - 1) < 1e-6;
			}
		}
	}
}
