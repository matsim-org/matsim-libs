package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

public class PretoriaTest {

	private static final Logger logger = LogManager.getLogger(PretoriaTest.class);

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO Files were changed to local for debugging purposes. Change them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	private final static String HBEFA_HGV_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_HGV_Aleks.csv";

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

		/*for(var node : cRoute.getNodes().values()){
			node.setCoord(new Coord(-node.getCoord().getX(), -node.getCoord().getY()));
		}*/
		NetworkUtils.writeNetwork(cRoute, "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/network_routeC_etios.xml");
	}

	// WGS83 -> SA_Lo29
	private final static GeotoolsTransformation TRANSFORMATION = new GeotoolsTransformation("EPSG:4326", "SA_Lo29");

	/// Returns coord in SA_Lo29 format, that the GPS coordinates maps to.
	private Coord convertWGS84toLo29(double gpsLat, double gpsLon){
		Coord coordWGS84 = new Coord(gpsLon, gpsLat);
		return TRANSFORMATION.transform(coordWGS84);
	}

	private static EmissionsConfigGroup getEmissionsConfigGroup(PretoriaVehicle vehicle) {
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
		ecg.setDetailedColdEmissionFactorsFile(HBEFA_COLD_DET);

		switch (vehicle){
			case PretoriaVehicle.ETIOS, PretoriaVehicle.FIGO -> ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
			case PretoriaVehicle.RRV -> ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HGV_HOT_DET);
		}

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
			case PretoriaVehicle.ETIOS -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-etios.csv");
			case PretoriaVehicle.FIGO -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-figo.csv");
			case PretoriaVehicle.RRV -> Path.of("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/data/public-rrv.csv");
		};

		String referenceDate = switch(vehicle){
			case PretoriaVehicle.ETIOS -> "2022-11-15T07:34:18.115Z";
			case PretoriaVehicle.FIGO -> "07/27/2021 10:03:03.131 +0200";
			case PretoriaVehicle.RRV -> "02/02/2021 10:09:42.773 +0200";
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

	/// Removes clusters, that happen due to standing times TODO Check if correct
	private List<PretoriaGPSEntry> clusterStandingGpsEntries(List<PretoriaGPSEntry> gpsEntries){
		List<PretoriaGPSEntry> cleanedEntries = new ArrayList<>(gpsEntries);

		int i = 2;
		while(i < cleanedEntries.size()){
			PretoriaGPSEntry start = cleanedEntries.get(i-2);
			PretoriaGPSEntry middle = cleanedEntries.get(i-1);
			PretoriaGPSEntry end = cleanedEntries.get(i);

			// Do not cluster between different trips
			if (!(start.trip == middle.trip && middle.trip == end.trip))
				throw new IllegalArgumentException("Tried to cluster entries from different driving trips!");

			// If the directions change too sharply, the entry is removed and its stats are added to the next entry
			Coord vec0 = CoordUtils.minus(cleanedEntries.get(i-2).coord, cleanedEntries.get(i-1).coord);
			Coord vec1 = CoordUtils.minus(cleanedEntries.get(i-1).coord, cleanedEntries.get(i).coord);

			// Check if the angle is larger than (or eq.) 90 degree OR if veh speed is less than 1 km/h
			if (vec0.getX() * vec1.getX() + vec0.getY() * vec1.getY() <= 0 || middle.vehVel < 1){
				// Angle is too sharp, save data of i-1
				double CO = cleanedEntries.get(i-1).CO;
				double CO2 = cleanedEntries.get(i-1).CO2;
				double NOx = cleanedEntries.get(i-1).NOx;

				// Create updated entry i
				var updatedEntry = new PretoriaGPSEntry(
					cleanedEntries.get(i).time,
					cleanedEntries.get(i).trip,
					cleanedEntries.get(i).driver,
					cleanedEntries.get(i).route,
					cleanedEntries.get(i).load,
					cleanedEntries.get(i).coldStart,
					cleanedEntries.get(i).coord,
					cleanedEntries.get(i).gpsAlt,
					cleanedEntries.get(i).gpsVel,
					cleanedEntries.get(i).vehVel,
					cleanedEntries.get(i).CO + CO,
					cleanedEntries.get(i).CO2 + CO2,
					cleanedEntries.get(i).NOx + NOx
					);

				// Update the list
				cleanedEntries.set(i, updatedEntry);
				cleanedEntries.remove(i-1);
			} else {
				i++;
			}
		}

		assert gpsEntries.stream().mapToDouble(e -> e.CO).reduce(Double::sum).getAsDouble() -
			cleanedEntries.stream().mapToDouble(e -> e.CO).reduce(Double::sum).getAsDouble() < 1e-6;
		assert gpsEntries.stream().mapToDouble(e -> e.CO2).reduce(Double::sum).getAsDouble() -
			cleanedEntries.stream().mapToDouble(e -> e.CO2).reduce(Double::sum).getAsDouble() < 1e-6;
		assert gpsEntries.stream().mapToDouble(e -> e.NOx).reduce(Double::sum).getAsDouble() -
			cleanedEntries.stream().mapToDouble(e -> e.NOx).reduce(Double::sum).getAsDouble() < 1e-6;

		return cleanedEntries;
	}

	///  Returns a list of interpolated points, interpolation happens every meter
	private List<PretoriaGPSEntry> interpolateGpsEntries(List<PretoriaGPSEntry> gpsEntries){
		List<PretoriaGPSEntry> interpolatedEntries = new LinkedList<>();
		interpolatedEntries.add(gpsEntries.getFirst());

		for (int i = 1; i < gpsEntries.size(); i++) {
			PretoriaGPSEntry start = gpsEntries.get(i-1);
			PretoriaGPSEntry end = gpsEntries.get(i);

			// Do not interpolate between different trips
			if (start.trip != end.trip)
				throw new IllegalArgumentException("Tried to interpolate two entries from different driving trips!");

			double distance = CoordUtils.calcEuclideanDistance(start.coord, end.coord);

			// Return an empty list, if the distance between the points is too small
			if (distance <= 1){
				interpolatedEntries.add(end);
				continue;
			}

			// Create one interpolation point per meter
			double traversedDistance = 0;
			for (double d = 1; d < distance; d += 1) {
				double interpTime = start.time + (d / distance) * (end.time - start.time);
				double interpAlt = start.gpsAlt + (d / distance) * (end.gpsAlt - start.gpsAlt);
				Coord interpCoord = CoordUtils.plus(start.coord, CoordUtils.scalarMult(d / distance, CoordUtils.minus(end.coord, start.coord)));

				interpolatedEntries.add(new PretoriaGPSEntry(
					interpTime,
					end.trip,
					end.driver,
					end.route,
					end.load,
					end.coldStart,
					interpCoord,
					interpAlt,
					0,
					0,
					(1 / distance) * end.CO,
					(1 / distance) * end.CO2,
					(1 / distance) * end.NOx
				));

				traversedDistance += 1;
			}

			// Add one last entry with remaining emissions (replacing the end point)
			interpolatedEntries.add(new PretoriaGPSEntry(
				end.time,
				end.trip,
				end.driver,
				end.route,
				end.load,
				end.coldStart,
				end.coord,
				end.gpsAlt,
				0,
				0,
				(1 - traversedDistance / distance) * end.CO,
				(1 - traversedDistance / distance) * end.CO2,
				(1 - traversedDistance / distance) * end.NOx
			));
		}

		assert gpsEntries.stream().mapToDouble(e -> e.CO).reduce(Double::sum).getAsDouble() -
			interpolatedEntries.stream().mapToDouble(e -> e.CO).reduce(Double::sum).getAsDouble() < 1e-6;
		assert gpsEntries.stream().mapToDouble(e -> e.CO2).reduce(Double::sum).getAsDouble() -
			interpolatedEntries.stream().mapToDouble(e -> e.CO2).reduce(Double::sum).getAsDouble() < 1e-6;
		assert gpsEntries.stream().mapToDouble(e -> e.NOx).reduce(Double::sum).getAsDouble() -
			interpolatedEntries.stream().mapToDouble(e -> e.NOx).reduce(Double::sum).getAsDouble() < 1e-6;

		return interpolatedEntries;
	}

	/**
	 * Returns the time spent on the start and endLink for two switching GPSEntries. Needed to accurately estimate the traveltime for each link,
	 * especially if velocity is high and/or the links are short.
	 * @param startPoint last gps point, that has been assigned to the startLink
	 * @param endPoint first gps point, that has been assigned to the endLink
	 * @param startLink prior traversed link (must incident to endlink)
	 * @param endLink post traversed link
	 * @return (t1,t2) with t1 being time spent on startLink and t2 being time spent on endLink.
	 */
	private Tuple<Double, Double> computeGPSEdgeTimes(PretoriaGPSEntry startPoint, PretoriaGPSEntry endPoint, Link startLink, Link endLink){
		Coord startProjection = CoordUtils.orthogonalProjectionOnLineSegment(startLink.getFromNode().getCoord(), startLink.getToNode().getCoord(), startPoint.coord);
		Coord endProjection = CoordUtils.orthogonalProjectionOnLineSegment(endLink.getFromNode().getCoord(), endLink.getToNode().getCoord(), endPoint.coord);
		double totalTime = endPoint.time - startPoint.time;

		// Get the node, that is shared by both links
		Coord sharedNode = null;
		if (startLink.getFromNode().equals(endLink.getFromNode()) || startLink.getFromNode().equals(endLink.getToNode())) {
			sharedNode = startLink.getFromNode().getCoord();
		} else if (startLink.getToNode().equals(endLink.getFromNode()) || startLink.getToNode().equals(endLink.getToNode())) {
			sharedNode = startLink.getToNode().getCoord();
		} else {
			logger.warn("The given links ({}, {}) are not incident!", startLink.getId(), endLink.getId());
			return new Tuple<>(totalTime/2, totalTime/2);
		}

		double startLength = CoordUtils.length(CoordUtils.minus(sharedNode, startProjection));
		double endLength = CoordUtils.length(CoordUtils.minus(endProjection, sharedNode));
		double totalLength = startLength + endLength;

		double startTime = totalTime*startLength/totalLength;
		double endTime = totalTime*endLength/totalLength;
		assert startTime + endTime - totalTime < 1e-6;
		return new Tuple<>(startTime, endTime);
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

	@ParameterizedTest
	@EnumSource(PretoriaVehicle.class)
	public void pretoriaInputsExpTest(PretoriaVehicle vehicle) throws IOException {
//		final String SVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

		// Prepare config
		EmissionsConfigGroup ecg = getEmissionsConfigGroup(vehicle);
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
		Map<Integer, List<PretoriaGPSEntry>> tripId2pretoriaGpsEntries = readGpsEntries(vehicle);
		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, clusterStandingGpsEntries(entries)));
//		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, interpolateGpsEntries(entries))); // TODO Makes results worse

		// Attach gps-information to the matsim links TODO Extract into seperate method
		Map<Integer, Map<Id<Link>, List<PretoriaGPSEntry>>> tripId2linkId2pretoriaGpsEntries = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Double>> tripId2linkId2traversalTime = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Map<Pollutant, Tuple<Double, Double>>>> tripId2linkId2pollutant2emissions = new ArrayMap<>();
		Map<Integer, Integer> tripId2load = new ArrayMap<>();

		// TODO debug only, remove these vars
		Map<PretoriaGPSEntry, Id<Link>> pretoriaGpsEntry2linkId = new HashMap<>();
		Set<Tuple<PretoriaGPSEntry, Id<Link>>> skippedEntries = new HashSet<>();

		for(var gpsTripEntries : tripId2pretoriaGpsEntries.entrySet()){

			Id<Link> previousLinkId = null;
			PretoriaGPSEntry previousGpsEntry = null;
			var tripId = gpsTripEntries.getKey();

			for(var gpsEntry : gpsTripEntries.getValue()){
				tripId2linkId2pretoriaGpsEntries.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2traversalTime.putIfAbsent(tripId, new HashMap<>());
				tripId2load.putIfAbsent(tripId, gpsEntry.load);

				var linkId = NetworkUtils.getNearestLinkExactly(pretoriaNetwork, gpsEntry.coord).getId();
				tripId2linkId2pretoriaGpsEntries.get(tripId).putIfAbsent(linkId, new ArrayList<>());
				tripId2linkId2pretoriaGpsEntries.get(tripId).get(linkId).add(gpsEntry);

				pretoriaGpsEntry2linkId.put(gpsEntry, linkId);

				// Check  if vehicle arrived at new link
				if(previousLinkId != linkId){
					Link startLink = pretoriaNetwork.getLinks().get(previousLinkId);
					Link endLink = pretoriaNetwork.getLinks().get(linkId);

					// Get the edgeTimes
					Tuple<Double, Double> edgeTimes = previousGpsEntry == null ? new Tuple<>(0., 0.) :
						computeGPSEdgeTimes(
							previousGpsEntry,
							gpsEntry,
							startLink,
							endLink
						);

					// Finish old entry
					if(previousLinkId != null)
						tripId2linkId2traversalTime.get(tripId).compute(previousLinkId, (k, arrivalTime) -> gpsEntry.time + edgeTimes.getFirst() - arrivalTime);

					// TODO Check if the skipped links cause problems later on!
					// Duplicate link entries are not allowed. However, it can happen that the last points gets assigned to the first link. We skip the next points.
					if(tripId2linkId2traversalTime.get(tripId).containsKey(linkId)) {
						if(!linkId.equals(Id.createLinkId("6555"))) {
							System.out.println(gpsEntry);
							throw new RuntimeException("Duplicate entry for non-start link! (linkId=" + linkId + ")");
						}

						skippedEntries.add(new Tuple<>(gpsEntry, linkId));

						break;
					}

					// Start new entry with arrival time
					tripId2linkId2traversalTime.get(tripId).put(linkId, gpsEntry.time - edgeTimes.getSecond());
				}

				previousLinkId = linkId;
				previousGpsEntry = gpsEntry;
			}
		}

		// Get the emission values (PEMS and MATSim)
		EmissionModule module = new EmissionModule(scenario, manager);

		for(var tripEntry : tripId2linkId2pretoriaGpsEntries.entrySet()){
			var tripId = tripEntry.getKey();

			for(var linkEntry : tripEntry.getValue().entrySet()){
				var linkId = linkEntry.getKey();
				var link = pretoriaNetwork.getLinks().get(linkId);

				// Extract the PEMS information
				double CO_pems = linkEntry.getValue().stream().mapToDouble(e -> e.CO).filter(d -> d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY).reduce(Double::sum).getAsDouble();
				double CO2_pems = linkEntry.getValue().stream().mapToDouble(e -> e.CO2).filter(d -> d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY).reduce(Double::sum).getAsDouble();
				double NOx_pems = linkEntry.getValue().stream().mapToDouble(e -> e.NOx).filter(d -> d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY).reduce(Double::sum).getAsDouble();

				// If HGV: Update size class
				if(vehicle.equals(PretoriaVehicle.RRV) && tripId2load.get(tripId) != 0){
					vehicleAttributes.setHbefaSizeClass("RT >32t"); // TODO Seems like no size class matches the PEMS values. Why?
				}

				// Compute the MATSim emissions
				// TODO Add cold emissions
				var emissionsMatsim = module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
					tripId2linkId2traversalTime.get(tripId).get(linkId),
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
			IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/pretoria/output_" + vehicle + ".csv"),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"tripId",
			"linkId",
			"load",
			"segment",
			"CO_MATSim",
			"CO_pems",
			"CO2_MATSim",
			"CO2_pems",
			"NOx_MATSim",
			"NOx_pems"
		);

		String segment = "none";
		for(var tripEntry : tripId2linkId2pollutant2emissions.entrySet()){
			var tripId = tripEntry.getKey();
			for(var linkEntry : tripEntry.getValue().entrySet()) {
				var linkId = linkEntry.getKey();
				var pollutantMap = linkEntry.getValue();

				if(linkId.equals(Id.createLinkId("28948")))
					segment = "A";
				if(linkId.equals(Id.createLinkId("14100")))
					segment = "none";

				if(linkId.equals(Id.createLinkId("11614")))
					segment = "B";
				if(linkId.equals(Id.createLinkId("28906")))
					segment = "none";

				if(linkId.equals(Id.createLinkId("waterkloof4_waterkloof5")))
					segment = "C";
				if(linkId.equals(Id.createLinkId("37156")))
					segment = "none";

				try {
					writer.printRecord(
						tripId,
						linkId,
						tripId2load.get(tripId),
						segment,
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
			}
		}

		writer.flush();
		writer.close();

		// TODO debug only, remove later
		CSVPrinter writer2 = new CSVPrinter(
			IOUtils.getBufferedWriter("/Users/aleksander/Documents/VSP/PHEMTest/pretoria/points_" + vehicle + ".csv"),
			CSVFormat.DEFAULT);
		writer2.printRecord(
			"time",
			"x",
			"y",
			"linkId"
		);

		tripId2pretoriaGpsEntries.get(2).forEach( e -> {
			try {
				writer2.printRecord(e.time, -e.coord.getX(), -e.coord.getY(), pretoriaGpsEntry2linkId.get(e));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});

		writer2.flush();
		writer2.close();

	}

	// ----- Helper definitions -----

	private record PretoriaGPSEntry(double time, int trip, int driver, int route, int load, boolean coldStart, Coord coord, double gpsAlt, double gpsVel, double vehVel, double CO, double CO2, double NOx){};

	public enum PretoriaVehicle{
		/// Toyota Etios 1.5 (1496ccm, 66kW) Sprint hatchback light passenger vehicle with a Euro 6 classification (138g/km) (file: public-etios.csv).
		ETIOS("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR),

		// TODO Euro 5 or Euro 6? (contradicting information)
		/// Ford Figo 1.5 (1498ccm, 91kW) Trend hatchback light passenger vehicle with a Euro 6 classification (132g/km) (file: public-figo.csv).
		FIGO("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR),

		//TODO Add load entry
		/// Isuzu FTR850 AMT (Road-Rail Vehicle) medium heavy vehicle with a Euro 3 classification (file: public-rrv.csv).
		RRV("diesel", "HGV D Euro-III", "RT >7.5-12t", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);

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
