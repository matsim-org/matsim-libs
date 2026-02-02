package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.stream.Stream;

public class PretoriaTest {

	private static final Logger logger = LogManager.getLogger(PretoriaTest.class);

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private final static String SVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

	// TODO Files were changed to local for debugging purposes. Change them back to the svn entries, when fixed hbefa tables are available
	private final static String HBEFA_4_1_PATH = "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/";
	private final static String HBEFA_HOT_AVG = HBEFA_4_1_PATH + "EFA_HOT_Vehcat_2020_Average.csv";
	private final static String HBEFA_COLD_AVG = HBEFA_4_1_PATH + "EFA_ColdStart_Vehcat_2020_Average.csv";
	private final static String HBEFA_HOT_DET = HBEFA_4_1_PATH + "EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv";
	private final static String HBEFA_COLD_DET = HBEFA_4_1_PATH + "EFA_ColdStart_Concept_2020_detailed_perTechAverage.csv";

	private final static String HBEFA_HOT_TECHAVG = HBEFA_4_1_PATH + "EFA_HOT_Concept_perTechFueltype_all_2020.csv";

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

	private static EmissionsConfigGroup getEmissionsConfigGroup(PretoriaVehicle vehicle, EmissionsConfigGroup.EmissionsComputationMethod method) {
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( method );
		switch (vehicle){
			case ETIOS, FIGO, RRV -> ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			case FIGO_TECHAVG, RRV_TECHAVG -> ecg.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort );
		}
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
			case ETIOS, FIGO -> ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_DET);
			case RRV -> ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HGV_HOT_DET);
			case FIGO_TECHAVG, RRV_TECHAVG -> ecg.setDetailedWarmEmissionFactorsFile(HBEFA_HOT_TECHAVG);
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

	/// Removes clusters, that happen due to standing times TODO Check if correct
	private List<PretoriaGPSEntry> clusterStandingGpsEntries(List<PretoriaGPSEntry> gpsEntries){
		/*List<PretoriaGPSEntry> cleanedEntries = new ArrayList<>(gpsEntries);

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

		return cleanedEntries;*/

		List<PretoriaGPSEntry> cleanedEntries = new ArrayList<>(gpsEntries);

		int i = 0;
		while(i < cleanedEntries.size()-1){
			PretoriaGPSEntry e = cleanedEntries.get(i);

			// Check if the angle is larger than (or eq.) 90 degree OR if veh speed is less than 1 km/h
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

	public static Stream<Arguments> pretoriaInputsExpTestParams(){
		var methods = List.of(
			EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction,
			EmissionsConfigGroup.EmissionsComputationMethod.InterpolationFraction,
			EmissionsConfigGroup.EmissionsComputationMethod.BilinearInterpolationFraction
		);

		return Stream.of(PretoriaVehicle.values())
			.flatMap(v -> methods.stream().map(m -> Arguments.of(v, m)));
	}

	@ParameterizedTest
	@MethodSource("pretoriaInputsExpTestParams")
	public void pretoriaInputsExpTest(PretoriaVehicle vehicle, EmissionsConfigGroup.EmissionsComputationMethod method) throws IOException {
//		final String SVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

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
		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, filterUnwantedLinkEntries(entries, unwantedLinks, pretoriaNetwork)));
		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, clusterStandingGpsEntries(entries)));
//		tripId2pretoriaGpsEntries.forEach((tripId, entries) -> tripId2pretoriaGpsEntries.put(tripId, interpolateGpsEntries(entries))); // TODO Makes results worse

		// TODO

		var linkOrder = getLinkOrder(tripId2pretoriaGpsEntries.get(1), pretoriaNetwork);
		var projectedAccumulatedDistances = getProjectedAccumulatedDistances(tripId2pretoriaGpsEntries.get(1), linkOrder, pretoriaNetwork);
		var kalmanFilter = new KalmanFilter(pretoriaNetwork, linkOrder, projectedAccumulatedDistances);
		List<Double> accumulatedDistances = kalmanFilter.naiveFilter(tripId2pretoriaGpsEntries.get(1));
		kalmanFilter.printCsv(tripId2pretoriaGpsEntries.get(1), "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/kalman/out.csv");

		// Attach gps-information to the matsim links TODO Extract into seperate method
		Map<Integer, Map<Id<Link>, List<PretoriaGPSEntry>>> tripId2linkId2pretoriaGpsEntries = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Double>> tripId2linkId2traversalTime = new ArrayMap<>();
		Map<Integer, Map<Id<Link>, Map<Pollutant, Tuple<Double, Double>>>> tripId2linkId2pollutant2emissions = new ArrayMap<>();
		Map<Integer, Map<Pollutant, Double>> tripId2pollutant2coldEmissions = new ArrayMap<>();
		Map<Integer, Integer> tripId2load = new ArrayMap<>();
		Map<Integer, Integer> tripId2driver = new ArrayMap<>();
		Map<Integer, Boolean> tripId2coldStart = new ArrayMap<>();

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
				tripId2driver.putIfAbsent(tripId, gpsEntry.driver);
				tripId2coldStart.putIfAbsent(tripId, gpsEntry.coldStart);

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

			// Calculate cold Emissions (but not for RRV, as RRV has no available HBEFA Cold Table)
			if(vehicle.hbefaVehicleCategory != HbefaVehicleCategory.HEAVY_GOODS_VEHICLE){
				vehHbefaInfo.getSecond().setHbefaEmConcept("average");
				var coldEmissionsMatsim = module.getColdEmissionAnalysisModule().calculateColdEmissions(
					Id.createVehicleId("0"),
					13*3600,
					vehHbefaInfo,
					2
				);

				tripId2pollutant2coldEmissions.putIfAbsent(tripId, new HashMap<>());
				tripId2pollutant2coldEmissions.get(tripId).put(Pollutant.CO, coldEmissionsMatsim.get(Pollutant.CO));
				tripId2pollutant2coldEmissions.get(tripId).put(Pollutant.CO2_TOTAL, coldEmissionsMatsim.get(Pollutant.CO2_TOTAL));
				tripId2pollutant2coldEmissions.get(tripId).put(Pollutant.NOx, coldEmissionsMatsim.get(Pollutant.NOx));
			}

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

				// Calculate warm emissions
				vehHbefaInfo.getSecond().setHbefaEmConcept(vehicle.hbefaEmConcept);
				var warmEmissionsMatsim = module.getWarmEmissionAnalysisModule().calculateWarmEmissions(
					tripId2linkId2traversalTime.get(tripId).get(linkId),
					(String) link.getAttributes().getAttribute("hbefa_road_type"),
					link.getFreespeed(),
					link.getLength(),
					vehHbefaInfo
				);

				double CO_matsim = warmEmissionsMatsim.get(Pollutant.CO);
				double CO2_matsim = warmEmissionsMatsim.get(Pollutant.CO2_TOTAL);
				double NOx_matsim = warmEmissionsMatsim.get(Pollutant.NOx);

				tripId2linkId2pollutant2emissions.putIfAbsent(tripId, new HashMap<>());
				tripId2linkId2pollutant2emissions.get(tripId).putIfAbsent(linkId, new HashMap<>());

				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.CO, new Tuple<>(CO_matsim, CO_pems));
				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.CO2_TOTAL, new Tuple<>(CO2_matsim, CO2_pems));
				tripId2linkId2pollutant2emissions.get(tripId).get(linkId).put(Pollutant.NOx, new Tuple<>(NOx_matsim, NOx_pems));
			}
		}



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
			"NOx_pems"
		);

		String segment = "none";
		for(var tripEntry : tripId2linkId2pollutant2emissions.entrySet()){
			var tripId = tripEntry.getKey();

			// Print the cold emissions if trip had a cold start
			if(tripId2coldStart.get(tripId) && vehicle.hbefaVehicleCategory != HbefaVehicleCategory.HEAVY_GOODS_VEHICLE){
				var coldPollutantMap = tripId2pollutant2coldEmissions.get(tripId);
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
						tripId2driver.get(tripId),
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

		/// Ford Figo 1.5 (1498ccm, 91kW) Trend hatchback light passenger vehicle with a Euro 6 classification (132g/km) (file: public-figo.csv).
		FIGO("petrol (4S)", "PC P Euro-6", "average", HbefaVehicleCategory.PASSENGER_CAR),
		FIGO_TECHAVG("petrol (4S)", "average", "average", HbefaVehicleCategory.PASSENGER_CAR),

		//TODO Add load entry
		/// Isuzu FTR850 AMT (Road-Rail Vehicle) medium heavy vehicle with a Euro 3 classification (file: public-rrv.csv).
		RRV("diesel", "HGV D Euro-III", "RT >7.5-12t", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE),
		RRV_TECHAVG("diesel", "average", "average", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);

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

	private static class KalmanFilter{
		// Needed for computation
		Network route;
		List<PretoriaGPSEntry> gpsEntries;
		List<Link> linkOrder;
		List<Double> projectedAccumulatedDistances;

		//Outputs
		List<Double> accumulatedDistances;
		List<Double> NAIVEaccumulatedDistance;

		public KalmanFilter(Network route, List<PretoriaGPSEntry> gpsEntries) {
			this.route = route;
			this.gpsEntries = gpsEntries;

			this.linkOrder = getLinkOrder();
			this.projectedAccumulatedDistances = getProjectedAccumulatedDistances();
		}

		/// Returns the links in between two links using BFS. TODO Check/Test
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
				if (cur.equals(startLink)) break;
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

		private Node getSharedNode(Link a, Link b){
			// Get the node, that is shared by both links
			Node sharedNode = null;
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
				List<Link> previousLinks = linkOrder.subList(0, linkOrder.indexOf(l));
				double prevLen = previousLinks.stream().mapToDouble(Link::getLength).sum();

				Coord p = CoordUtils.orthogonalProjectionOnLineSegment(l.getFromNode().getCoord(), l.getToNode().getCoord(), e.coord);
				Node sharedNode = previousLinks.isEmpty() ? l.getFromNode() : getSharedNode(l, previousLinks.getLast());
				double projectedLen = CoordUtils.length(CoordUtils.minus(sharedNode.getCoord(), p));

				projectedAccumulatedDistances.add(prevLen + projectedLen);
			}
			return projectedAccumulatedDistances;
		}

		// TODO Check why the kalmanFilter overshoots
		private List<Double> kalmanFilter() {
			List<Double> accumulatedDistances = new ArrayList<>(gpsEntries.size());
			accumulatedDistances.add(0.);
			List<Double> NAIVEaccumulatedDistance = new ArrayList<>(gpsEntries.size());
			NAIVEaccumulatedDistance.add(0.);

			// Init the state variables
			double S = 0;
			double V = gpsEntries.getFirst().vehVel;

			// Init the process noise covariance matrix
			double p00 = 25; // stderr=5m/s
			double p01 = 0;
			double p11 = 4; // stderr=2m/s^2

			// uncertainties TODO Compute them from dataset instead of setting them arbitrarily
			double sigma_a = 2.0;
			double sigma_v = 0.2;
			double sigma_gps = 6;

			double z_S;
			double z_V = 0;
			for (int i = 1; i < gpsEntries.size(); i++) {
				// Step 1: Prediction
				double dt = gpsEntries.get(i).time - gpsEntries.get(i-1).time;
				double dz = projectedAccumulatedDistances.get(i) - projectedAccumulatedDistances.get(i-1);

				// State transition by applying movement equation
				S = S + V*dt;

				// Create observation noise covariance matrix
				final double q00 = 0.25 * dt*dt*dt*dt * sigma_a*sigma_a;
				final double q01 = 0.5 * dt*dt*dt * sigma_a*sigma_a;
				final double q11 = dt*dt * sigma_a*sigma_a;

				// Update process noise covariance matrix
				double p00_new = p00 + dt*2*p01 + dt*dt*p11 + q00;
				double p01_new = p01 + dt*p11 + q01;
				double p11_new = p11 + q11;

				p00 = p00_new;
				p01 = p01_new;
				p11 = p11_new;

				// Step 2: Correction ( Distance )
				z_S = projectedAccumulatedDistances.get(i);

				// TODO lateral error
				double sigma_s = sigma_gps; // + e_lateral;
				double R = sigma_s*sigma_s;

				double y = z_S - S;

				double S_cov = p00 + R;

				double K0 = p00 / S_cov;
				double K1 = p01 / S_cov;

				// Update state
				S = S + K0 * y;

				double p00_old = p00;
				double p01_old = p01;
				double p11_old = p11;

				// Update covariance
				p00 = p00_old - K0 * p00_old;
				p01 = p01_old - K0 * p01_old;
				p11 = p11_old - K1 * p01_old;

				// Step 3: Correction ( Velocity )
				z_V = gpsEntries.get(i).vehVel;

				R = sigma_v * sigma_v;

				y = z_V - V;

				S_cov = p11 + R;

				K0 = p01 / S_cov;
				K1 = p11 / S_cov;

				V = V + K1 * y;

				p00_old = p00;
				p01_old = p01;
				p11_old = p11;

				p00 = p00_old - K0 * p01_old;
				p01 = p01_old - K0 * p11_old;
				p11 = p11_old - K1 * p11_old;

				// Step 4 Save the distance in a map
				accumulatedDistances.add(S);

				if(V < 0 ) V = 0;

				// TODO DEBUG Only
				double naiveLen = CoordUtils.length(CoordUtils.minus(gpsEntries.get(i).coord, gpsEntries.get(i-1).coord));
				NAIVEaccumulatedDistance.add(NAIVEaccumulatedDistance.getLast() + naiveLen);
			}

			this.accumulatedDistances = accumulatedDistances;
			this.NAIVEaccumulatedDistance = NAIVEaccumulatedDistance;

			return accumulatedDistances;
		}

		// TODO Temporary solution
		private List<Double> naiveFilter(){
			NAIVEaccumulatedDistance = new ArrayList<>(gpsEntries.size());

			for (int i = 1; i < gpsEntries.size(); i++){
				double naiveLen = CoordUtils.length(CoordUtils.minus(gpsEntries.get(i).coord, gpsEntries.get(i-1).coord));
				NAIVEaccumulatedDistance.add(NAIVEaccumulatedDistance.getLast() + naiveLen);
			}

			return NAIVEaccumulatedDistance;
		}

		private Map<Id<Link>, Map<Pollutant, Double>> computePollutantMap(){
			// TODO
			return null;
		}

		private void printCsv(List<PretoriaGPSEntry> gpsEntries, String path) throws IOException {
			CSVPrinter writer = new CSVPrinter(
				IOUtils.getBufferedWriter(path),
				CSVFormat.DEFAULT);
			writer.printRecord(
				"time",
				"accDist",
				"naiveAccDist",
				"projectedAccDist"
			);

			for(int i = 0; i < gpsEntries.size(); i++){
				writer.printRecord(
					gpsEntries.get(i).time,
					accumulatedDistances.get(i),
					NAIVEaccumulatedDistance.get(i),
					projectedAccumulatedDistances.get(i)
				);
			}

			writer.flush();
			writer.close();
		}

	}
}
