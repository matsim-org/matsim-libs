/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.polettif.multiModalMap.gtfs;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.multiModalMap.gtfs.containers.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


/**
 * Based on GTFS2MATSimTransitSchedule
 *
 * @author polettif
 */
public class GTFSReader {

	private static final Logger log = Logger.getLogger(GTFSReader.class);


	private final String root;
	private final CoordinateTransformation coordinateTransformation;
	private final String serviceIdsAlgorithm;
	private Map<String, Stop> stops;
	private SortedMap<String, Route> routes;
	private Map<String, Service> services;    //	The calendar services
	private Map<String, Shape> shapes;
	private boolean usesShapes;
	private boolean usesFrequencies;
	private boolean usesFrequenciesWarn = true;
	private String[] serviceIds;    //	The types of dates that will be represented by the new file

	// TODO serviceId which algorithm param

	TransitScheduleFactory transitScheduleFactory;
	TransitSchedule transitSchedule;

	public static final Id<Link> DUMMY_LINK = Id.createLinkId("DUMMY_LINK");
	public static final Id<Link> DUMMY_LINK_END = Id.createLinkId("DUMMY_LINK_END");
	public static final Id<Node> DUMMY_NODE_1 = Id.createNodeId("DUMMY_NODE_1");
	public static final Id<Node> DUMMY_NODE_2 = Id.createNodeId("DUMMY_NODE_2");
	public static final Id<Node> DUMMY_NODE_3 = Id.createNodeId("DUMMY_NODE_3");
	public static final String SERVICE_ID_MOST_USED = "mostused";

	private SimpleDateFormat timeFormat;
	private Map<String, Integer> serviceIdsCount;
	private boolean useDummyLinks = false;

	public static void main(final String[] args) {
		convertGTFS2MATSimTransitSchedule(args[0], args[1]);
	}

	/**
	 * Reads gtfs files in and converts them to an unmapped MATSim Transit Schedule.
	 * "Unmapped" means stopFacilities are not referenced to links and transit lines do not have routes. The schedule is
	 * valid however since dummy link ids are used.
	 *
	 * GTFS and the unmapped schedule are in WGS84, no coordinate transformation is applied.
	 *
	 * @param gtfsInputPath folder where the gtfs files are located
	 * @param mtsOutputFile path to the (to be generated) unmapped transit schedule file
	 */
	public static void convertGTFS2MATSimTransitSchedule(String gtfsInputPath, String mtsOutputFile) {
		GTFSReader gtfsReader = new GTFSReader(gtfsInputPath);

		gtfsReader.writeTransitSchedule(mtsOutputFile);
	}

	public GTFSReader(String inputPath, String outCoordinateSystem) {
		if(!(inputPath.substring(inputPath.length() - 1).equals("/")))
			inputPath += "/";

		// TODO outCoordinateSystem is not yet used

		// loading files
		this.stops = new HashMap<>();
		this.services = new HashMap<>();
		this.routes = new TreeMap<>();
		this.shapes = new HashMap<>();
		this.root = inputPath;
		this.usesShapes = false;
		this.usesFrequencies = false;
		this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outCoordinateSystem);
		this.timeFormat = new SimpleDateFormat("HH:mm:ss");
		this.serviceIdsCount = new HashMap<>();

		// TODO there is only one algorithm to date
		this.serviceIdsAlgorithm = SERVICE_ID_MOST_USED;

		// to convert to basic matsim transit schedule
		this.transitScheduleFactory = new TransitScheduleFactoryImpl();
		this.transitSchedule = transitScheduleFactory.createTransitSchedule();

		// run
		this.loadFiles();
		this.convert();
	}

	public GTFSReader(String inputPath) {
		this(inputPath, "WGS84");
	}

	public void writeTransitSchedule(String outputPath) {
		new TransitScheduleWriterV1(transitSchedule).write(outputPath);
	}

	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}

	/**
	 * Converts the loaded gtfs data to a matsim transit schedule
	 */
	private void convert() {

		// TODO set service IDs with input param or define algorithm
		setServiceIds(SERVICE_ID_MOST_USED);

		// generating mts stops from gtfs stops
		// assign dummy link id
		for(Entry<String, Stop> stop: stops.entrySet()) {
			Coord result = stop.getValue().getPoint(); // TODO coordinateTransformation.transform(stop.getValue().getPoint());
			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create(stop.getKey(), TransitStopFacility.class), result, stop.getValue().isBlocks());
			transitStopFacility.setName(stop.getValue().getName());
			transitSchedule.addStopFacility(transitStopFacility);
			if(useDummyLinks) {transitStopFacility.setLinkId(DUMMY_LINK); }
		}

		// use unique departure ids (safer than generating one for each transitRoue as long as Departures are stored in a Map
		int departureId = 0;

		for(Map.Entry<String,Route> route:routes.entrySet()) {
			// creating new transit line
			TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create(route.getKey(), TransitLine.class));
			transitSchedule.addTransitLine(transitLine);

			// foreach trip
			for(Map.Entry<String, Trip> trip : route.getValue().getTrips().entrySet()) {
				boolean isService=false;

				// if trip is part of used serviceId
				for(String serviceId:serviceIds) {
					if (trip.getValue().getService().equals(services.get(serviceId))) {
						isService = true;
					}
				}

				if(isService) {
					// get stop sequence
					List<TransitRouteStop> transitRouteStops = new ArrayList<>();
					Date startTime = trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getArrivalTime();
					for (Integer stopTimeKey : trip.getValue().getStopTimes().keySet()) {
						StopTime stopTime = trip.getValue().getStopTimes().get(stopTimeKey);
						double arrival = Time.UNDEFINED_TIME, departure = Time.UNDEFINED_TIME;
						if (!stopTimeKey.equals(trip.getValue().getStopTimes().firstKey())) {
							long difference = stopTime.getArrivalTime().getTime() - startTime.getTime();
							try {
								arrival = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime() + difference)));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						if (!stopTimeKey.equals(trip.getValue().getStopTimes().lastKey())) {
							long difference = stopTime.getDepartureTime().getTime() - startTime.getTime();
							try {
								departure = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime() + difference)));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						transitRouteStops.add(transitScheduleFactory.createTransitRouteStop(transitSchedule.getFacilities().get(Id.create(stopTime.getStopId(), TransitStopFacility.class)), arrival, departure));
					}

					if(usesFrequencies && usesFrequenciesWarn) {
						log.warn("Algorithm does not yet support frequencies instead of stop_times to create departure times!");
						usesFrequenciesWarn = false;
					}

					// if route sequence is already used by the same transitLine: just add new departure for transitRoute
					boolean routeExistsInTransitLine = false;
					for (Entry<Id<TransitRoute>, TransitRoute> routeEntry : transitLine.getRoutes().entrySet()) {
						if (routeEntry.getValue().getStops().equals(transitRouteStops)) {
							routeEntry.getValue().addDeparture(transitScheduleFactory.createDeparture(
									Id.create(departureId++, Departure.class), Time.parseTime(timeFormat.format(startTime))));
							routeExistsInTransitLine = true;
							break;
						}
					}

					if (!routeExistsInTransitLine) {
						TransitRoute newTransitRoute = transitScheduleFactory.createTransitRoute(Id.create(trip.getKey(), TransitRoute.class), null, transitRouteStops, route.getValue().getRouteType().name);

						newTransitRoute.addDeparture(transitScheduleFactory.createDeparture(
								Id.create(departureId++, Departure.class), Time.parseTime(timeFormat.format(startTime))));
						transitLine.addRoute(newTransitRoute);
					}


				}
			} // foreach trip
		} // foreach route

		// Validate TransitSchedule with dummy network
		if(TransitScheduleValidator.validateAll(transitSchedule, createDummyNetwork()).isValid()) {
			log.info("Basic transit schedule is valid. However, stopFacilities are not yet referenced to links and transitLines do not include routes (link sequences).");
			log.info("############################################");
			log.info("GTFS successfully converted to basic MATSIM transit schedule");
		} else {
			log.error("Transit schedule not valid!");
		}

	}

	/**
	 * 	Calls all methods to load the gtfs files
	 *
	 * 	(loading methods ordered as in gtfs2matsimtransitschedule)
	 */
	private void loadFiles() {
		try {
			log.info("Loading gtfs files from "+root);
			loadStops();
			loadCalendar();
			loadCalendarDates();
			loadShapes();
			loadRoutes();
			loadTrips();
			loadStopTimes();
			loadFrequencies();
			log.info("All files loaded");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadStops() throws IOException {
		log.info("Loading stops.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"stops.txt"));
		String[] header = reader.readNext(); // read header
		Map<String, Integer> col = getIndices(header, new String[]{"stop_id", "stop_name", "stop_lat", "stop_lon"}); // get column numbers for required fields

		String[] line = reader.readNext();
		while (line != null) {
			Coord coord = new Coord(Double.parseDouble(line[col.get("stop_lon")]), Double.parseDouble(line[col.get("stop_lat")]));
			Stop stop = new Stop(coord, line[col.get("stop_name")], false);
			stops.put(line[col.get("stop_id")], stop);

			line = reader.readNext();
		}

		reader.close();
		log.info("...     stops.txt loaded");
	}

	private void loadCalendar() throws IOException {
		log.info("Loading calendar.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"calendar.txt"));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, new String[]{"service_id", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "start_date", "end_date"});

		// assuming all days follow monday in the file
		int indexMonday = col.get("monday");

		String[] line = reader.readNext();
		while (line != null) {
			boolean[] days = new boolean[7];
			for (int d = indexMonday; d < days.length; d++) {
				days[d] = line[d].equals("1");
			}
			// TODO analyse what's happening with services
			services.put(line[col.get("service_id")], new Service(days, line[col.get("start_date")], line[col.get("end_date")]));


			line = reader.readNext();
		}

		reader.close();
		log.info("...     calendar.txt loaded");
	}

	private void loadCalendarDates() throws IOException {
		log.info("Loading calendar_dates.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"calendar_dates.txt"));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, new String[]{"service_id", "date", "exception_type"});

		String[] line = reader.readNext();
		while (line != null) {
			// TODO: reassigning actual to services?
			Service actual = services.get(line[col.get("service_id")]);
			if (line[col.get("exception_type")].equals("2"))
				actual.addException(line[col.get("date")]);
			else
				actual.addAddition(line[col.get("date")]);

			line = reader.readNext();
		}

		reader.close();
		log.info("...     calendar_dates.txt loaded");
	}
	private void loadShapes()  {
		log.info("Looking for shapes.txt");
		// shapes are optional
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(root + "shapes.txt"));

			String[] header = reader.readNext();
			Map<String, Integer> col = getIndices(header, new String[]{"shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence"});

			String[] line = reader.readNext();
			while (line != null) {
				usesShapes = true; // shape file might exists but could be empty

				Shape actual = shapes.get(line[col.get("shape_id")]);
				if (actual == null) {
					actual = new Shape(line[col.get("shape_id")]);
					shapes.put(line[col.get("shape_id")], actual);
				}
				actual.addPoint(new Coord(Double.parseDouble(line[col.get("shape_pt_lat")]), Double.parseDouble(line[col.get("shape_pt_lon")])), Integer.parseInt(line[col.get("shape_pt_sequence")]));
				line = reader.readNext();
			}
			log.info("...     shapes.txt loaded");
		} catch (IOException e) {
			log.info("...     no shapes file found.");
		}
	}


	private void loadRoutes() throws IOException {
		log.info("Loading routes.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"routes.txt"));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, new String[]{"route_id", "route_short_name", "route_type"});

		String[] line = reader.readNext();
		int i=0;
		while (line != null) {
			Route route = new Route(line[col.get("route_short_name")], GTFSDefinitions.RouteTypes.values()[Integer.parseInt(line[col.get("route_type")])]);
			routes.put(line[col.get("route_id")], route);

			line = reader.readNext();
		}
		reader.close();
		log.info("...     routes.txt loaded");
	}

	private void loadTrips() throws IOException {
		log.info("Loading trips.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"trips.txt"));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, new String[]{"route_id", "service_id", "trip_id", "shape_id"});

		String[] line = reader.readNext();
		while (line != null) {
			Route route = routes.get(line[col.get("route_id")]);
			if(usesShapes)
				route.putTrip(line[col.get("trip_id")], new Trip(services.get(line[col.get("service_id")]), shapes.get(line[col.get("shape_id")]),line[col.get("trip_id")]));
			else
				route.putTrip(line[col.get("trip_id")], new Trip(services.get(line[col.get("service_id")]), null,line[col.get("trip_id")]));

			// each trip uses one service id, increase statistics accordingly
			if(serviceIdsAlgorithm.equals(SERVICE_ID_MOST_USED)) {
				Integer count = serviceIdsCount.get(line[col.get("service_id")]);
				if (count == null)
					serviceIdsCount.put(line[col.get("service_id")], 1);
				else
					serviceIdsCount.put(line[col.get("service_id")], count + 1);
			}
			line = reader.readNext();
		}

		reader.close();
		log.info("...     trips.txt loaded");
	}

	private void loadStopTimes() throws IOException {
		log.info("Loading stop_times.txt");
		CSVReader reader = new CSVReader(new FileReader(root+"stop_times.txt"));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, new String[]{"trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence"});

		String[] line = reader.readNext();
		int i=1;
		int c=1;
		while (line != null) {
			if(i == Math.pow(2, c)) { log.info("        line #"+i);	c++; } i++;

			for(Route actualRoute : routes.values()) {
				Trip trip = actualRoute.getTrips().get(line[col.get("trip_id")]);
				if(trip!=null) {
					try {
						trip.putStopTime(
								Integer.parseInt(line[col.get("stop_sequence")]),
								new StopTime(timeFormat.parse(line[col.get("arrival_time")]),
										timeFormat.parse(line[col.get("departure_time")]),
										line[col.get("stop_id")]));
					} catch (NumberFormatException | ParseException e) {
						e.printStackTrace();
					}
				}
			}
			line = reader.readNext();
		}

		reader.close();
		log.info("...     stop_times.txt loaded");
	}

	private void loadFrequencies() {
		log.info("Looking for frequencies.txt");
		// frequencies are optional
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(root + "frequencies.txt"));

			String[] header = reader.readNext();
			Map<String, Integer> col = getIndices(header, new String[]{"trip_id", "start_time", "end_time", "headway_secs"});

			String[] line = reader.readNext();
			while (line != null) {
				usesFrequencies = true;	// frequencies file might exists but could be empty

				for(Route actualRoute:routes.values()) {
					Trip trip = actualRoute.getTrips().get(line[col.get("trip_id")]);
					if(trip!=null) {
						try {
							trip.addFrequency(new Frequency(timeFormat.parse(line[col.get("start_time")]), timeFormat.parse(line[col.get("end_time")]), Integer.parseInt(line[col.get("headway_secs")])));
						} catch (NumberFormatException | ParseException e) {
							e.printStackTrace();
						}
					}
				}
				line = reader.readNext();
			}
			reader.close();
			log.info("...     frequencies.txt loaded");
		} catch (FileNotFoundException e1) {
			log.info("...     no frequencies.txt found.");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * In case optional columns are missing (or are out of order) in the csv file, adressing array
	 * values directly via integer does not work.
	 * @return indices
	 */
	public static Map<String, Integer> getIndices(String[] header, String[] columnNames) {
		Map<String, Integer> indices = new HashMap<>();

		for(String columnName : columnNames) {
			for(int i=0; i<header.length; i++) {
				if (header[i].equals(columnName)) {
					indices.put(columnName, i);
					break;
				}
			}
		}

		if(columnNames.length != indices.size())
			log.warn("Column name not found in csv. Might be some additional characters in the header or the encoding not being UTF-8.");

		return indices;
	}

	/**
	 * Generate a network with dummy links so the schedule can be validated.
	 *
	 * @author polettif
	 */
	private static Network createDummyNetwork() {

		Network network = NetworkUtils.createNetwork();

		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);

		network.addNode(networkFactory.createNode(DUMMY_NODE_1, new Coord(0, 1)));
		network.addNode(networkFactory.createNode(DUMMY_NODE_2, new Coord(0, 2)));
		network.addNode(networkFactory.createNode(DUMMY_NODE_3, new Coord(0, 3)));

		network.addLink(networkFactory.createLink(DUMMY_LINK, network.getNodes().get(DUMMY_NODE_1), network.getNodes().get(DUMMY_NODE_2)));
		network.addLink(networkFactory.createLink(DUMMY_LINK_END, network.getNodes().get(DUMMY_NODE_2), network.getNodes().get(DUMMY_NODE_3)));

		return network;
	}

	private void setServiceIds(String mode) {

		if(mode.equals(SERVICE_ID_MOST_USED)) {
			log.info("Getting most used service ID");

			serviceIds = new String[1];
			serviceIds[0] = getKeyOfMaxValue(serviceIdsCount);

			log.info("... serviceId: " + serviceIds[0] + " (" + serviceIdsCount.get(serviceIds[0]) + " occurences)");
		}
		else {
			log.warn("Using all service IDs (probably way too much data)");

			int i = 0;
			serviceIds = new String[services.size()];
			for (String s : services.keySet()) {
				serviceIds[i] = s;
				i++;
			}
		}
	}

	public void useDummyLinks(boolean b) {
		this.useDummyLinks = b;
	}

	/**
	 * Identifies the most used sevice ID of the validity period of the schedule and returns the ID.
	 *
	 * @author polettif
	 */
	private static String getKeyOfMaxValue(Map<String, Integer> map) {
		return map.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	}

}