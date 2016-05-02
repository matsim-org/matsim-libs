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
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.gtfs.containers.*;
import playground.polettif.multiModalMap.gtfs.containers.Frequency;
import playground.polettif.multiModalMap.gtfs.containers.GTFSDefinitions;
import playground.polettif.multiModalMap.gtfs.containers.Service;
import playground.polettif.multiModalMap.gtfs.containers.Shape;
import playground.polettif.multiModalMap.gtfs.containers.StopTime;
import playground.polettif.multiModalMap.gtfs.containers.Trip;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


/**
 * Reads GTFS files and converts them to an unmapped MATSim Transit Schedule
 * <p/>
 * Based on GTFS2MATSimTransitSchedule by Sergio Ordonez
 *
 * @author polettif
 */
public class GTFSReader {

	private static final Logger log = Logger.getLogger(GTFSReader.class);

	/**
	 * which algorithm should be used to get serviceIds
	 */
	private final String serviceIdsAlgorithm;
	public static final String SERVICE_ID_MOST_USED = "mostused";

	/**
	 * Path to the folder where the gtfs files are located
	 */
	private final String root;

	/**
	 * The types of dates that will be represented by the new file
	 */
	private String[] serviceIds;

	/**
	 * whether the gtfs feed uses frequencies.txt or not
	 */
	private boolean usesFrequencies = false;

	/**
	 * whether the gtfs feed uses shapes or not
	 */
	private boolean usesShapes = false;

	/**
	 * The time format used in the output MATSim transit schedule
	 */
	private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	/**
	 * map for counting how many trips use each serviceId
	 */
	private Map<String, Integer> serviceIdsCount = new HashMap<>();

	// containers for storing gtfs data
	private Map<String, GTFSStop> gtfsStops = new HashMap<>();
	private SortedMap<String, GTFSRoute> gtfsRoutes = new TreeMap<>();
	private Map<String, Service> services = new HashMap<>();
	private Map<String, Shape> shapes = new HashMap<>();
	private TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
	private TransitSchedule schedule = scheduleFactory.createTransitSchedule();

	/**
	 * Calls {@link #convertGTFS2MATSimTransitSchedule(String gtfsInputPath, String mtsOutputFile)}.
	 */
	public static void main(final String[] args) {
		convertGTFS2MATSimTransitSchedule(args[0], args[1]);
	}

	/**
	 * Reads gtfs files in and converts them to an unmapped MATSim Transit Schedule (mts).
	 * "Unmapped" means stopFacilities are not referenced to links and transit routes do not have routes (link sequences).
	 * <p/>
	 * GTFS and the unmapped schedule are both in WGS84, no coordinate transformation is applied.
	 *
	 * @param gtfsInputPath folder where the gtfs files are located (a single zip file is not yet supported)
	 * @param mtsOutputFile path to the (to be generated) unmapped transit schedule file
	 */
	public static void convertGTFS2MATSimTransitSchedule(String gtfsInputPath, String mtsOutputFile) {
		GTFSReader gtfsReader = new GTFSReader(gtfsInputPath);

		gtfsReader.writeTransitSchedule(mtsOutputFile);
	}

	public GTFSReader(String inputPath) {
		this.root = inputPath;

		// TODO there is only one algorithm to date
		this.serviceIdsAlgorithm = SERVICE_ID_MOST_USED;

		// run
		this.loadFiles();
		this.convert();
	}

	public void writeTransitSchedule(String outputPath) {
		new TransitScheduleWriter(schedule).writeFile(outputPath);
		log.info("MATSim Transit Schedule written to " + outputPath);
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}

	/**
	 * Converts the loaded gtfs data to a matsim transit schedule
	 * <ol>
	 * <li>generate transitStopFacilities from gtfsStops</li>
	 * <li>Create a transitLine for each GTFSRoute</li>
	 * <li>Generate a transitRoute for each trip</li>
	 * <li>Get the stop sequence of the trip</li>
	 * <li>Calculate departures from stopTimes or frequencies</li>
	 * <li>add transitRoute to the transitLine and thus to the schedule</li>
	 * </ol>
	 */
	private void convert() {

		log.info("Converting to MATSim transit schedule ...");

		// TODO set service IDs with input param or define algorithm
		setServiceIds(SERVICE_ID_MOST_USED);

		/** [1]
		 * generating transitStopFacilities (mts) from gtfsStops and add them to the schedule
		 */
		for(Entry<String, GTFSStop> stop : gtfsStops.entrySet()) {
			Coord result = stop.getValue().getPoint();
			TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(Id.create(stop.getKey(), TransitStopFacility.class), result, stop.getValue().isBlocks());
			stopFacility.setName(stop.getValue().getName());
			schedule.addStopFacility(stopFacility);
		}

		if(usesFrequencies) {
			log.info("        Using frequencies.txt to generate departures");
		} else {
			log.info("        Using stop_times.txt to generate departures");
		}

		DepartureIds departureIds = new DepartureIds();

		for(GTFSRoute gtfsRoute : gtfsRoutes.values()) {

			/** [2]
			 * Create a MTS transitLine for each GTFSRoute
			 */
			TransitLine transitLine = scheduleFactory.createTransitLine(Id.create(gtfsRoute.getRouteId(), TransitLine.class));
			schedule.addTransitLine(transitLine);

			/** [3]
			 * loop through each trip for the GTFSroute and generate transitRoute (if the serviceId is correct)
			 */
			for(Trip trip : gtfsRoute.getTrips().values()) {
				boolean isService = false;

				// if trip is part of used serviceId
				for(String serviceId : serviceIds) {
					if(trip.getService().equals(services.get(serviceId))) {
						isService = true;
					}
				}

				if(isService) {
					/** [4]
					 * Get the stop sequence (with arrivalOffset and departureOffset) of the trip.
					 */
					List<TransitRouteStop> transitRouteStops = new ArrayList<>();
					Date startTime = trip.getStopTimes().get(trip.getStopTimes().firstKey()).getArrivalTime();
					for(StopTime stopTime : trip.getStopTimes().values()) {
						double arrival = Time.UNDEFINED_TIME, departure = Time.UNDEFINED_TIME;

						// add arrival time if current stopTime is not on the first stop of the route
						if(!stopTime.getSeuencePosition().equals(trip.getStopTimes().firstKey())) {
							long difference = stopTime.getArrivalTime().getTime() - startTime.getTime();
							try {
								arrival = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime() + difference)));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}

						// add departure time if current stopTime is not on the last stop of the route
						if(!stopTime.getSeuencePosition().equals(trip.getStopTimes().lastKey())) {
							long difference = stopTime.getDepartureTime().getTime() - startTime.getTime();
							try {
								departure = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime() + difference)));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						transitRouteStops.add(scheduleFactory.createTransitRouteStop(schedule.getFacilities().get(Id.create(stopTime.getStopId(), TransitStopFacility.class)), arrival, departure));
					}

					/** [5.1]
					 * Calculate departures from frequencies (if available)
					 */
					if(usesFrequencies) {

						TransitRoute newTransitRoute = scheduleFactory.createTransitRoute(Id.create(trip.getId(), TransitRoute.class), null, transitRouteStops, gtfsRoute.getRouteType().name);

						for(Frequency frequency : trip.getFrequencies()) {
							for(Date actualTime = (Date) frequency.getStartTime().clone(); actualTime.before(frequency.getEndTime()); actualTime.setTime(actualTime.getTime() + frequency.getSecondsPerDeparture() * 1000)) {
								newTransitRoute.addDeparture(scheduleFactory.createDeparture(
										Id.create(departureIds.getNext(newTransitRoute.getId()), Departure.class),
										Time.parseTime(timeFormat.format(actualTime))));
							}
						}
						transitLine.addRoute(newTransitRoute);
					} else {
						/** [5.2]
						 * Calculate departures from stopTimes
						 */

						/* if stop sequence is already used by the same transitLine: just add new departure for the
						 * 	transitRoute that uses that stop sequence
						 */
						boolean routeExistsInTransitLine = false;
						for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
							if(transitRoute.getStops().equals(transitRouteStops)) {
								transitRoute.addDeparture(scheduleFactory.createDeparture(Id.create(departureIds.getNext(transitRoute.getId()), Departure.class), Time.parseTime(timeFormat.format(startTime))));
								routeExistsInTransitLine = true;
								break;
							}
						}

						/* if stop sequence is not used yet, create a new transitRoute (with transitRouteStops)
						 * and add the departure
						 */
						if(!routeExistsInTransitLine) {
							TransitRoute newTransitRoute = scheduleFactory.createTransitRoute(Id.create(trip.getId(), TransitRoute.class), null, transitRouteStops, gtfsRoute.getRouteType().name);

							newTransitRoute.addDeparture(scheduleFactory.createDeparture(Id.create(departureIds.getNext(newTransitRoute.getId()), Departure.class), Time.parseTime(timeFormat.format(startTime))));

							transitLine.addRoute(newTransitRoute);
						}
					}
				}
			} // foreach trip
		} // foreach route

		log.info("...     GTFS converted to an unmapped MATSIM Transit Schedule");
		log.info("#############################################################");
	}

	/**
	 * Calls all methods to load the gtfs files
	 */
	private void loadFiles() {
		try {
			log.info("Loading GTFS files from " + root);
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

	/**
	 * Reads all stops and puts them in {@link #gtfsStops}
	 * <p/>
	 * <br/><br/>
	 * stops.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Individual locations where vehicles pick up or drop off passengers.
	 *
	 * @throws IOException
	 */
	private void loadStops() throws IOException {
		log.info("Loading stops.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.STOPS.fileName));
		String[] header = reader.readNext(); // read header
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.STOPS.columns); // get column numbers for required fields

		String[] line = reader.readNext();
		while(line != null) {
			Coord coord = new Coord(Double.parseDouble(line[col.get("stop_lon")]), Double.parseDouble(line[col.get("stop_lat")]));
			GTFSStop GTFSStop = new GTFSStop(coord, line[col.get("stop_name")], false);
			gtfsStops.put(line[col.get("stop_id")], GTFSStop);

			line = reader.readNext();
		}

		reader.close();
		log.info("...     stops.txt loaded");
	}

	/**
	 * Reads all services and puts them in {@link #services}
	 * <p/>
	 * <br/><br/>
	 * calendar.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Dates for service IDs using a weekly schedule. Specify when service starts and ends,
	 * as well as days of the week where service is available.
	 *
	 * @throws IOException
	 */
	private void loadCalendar() throws IOException {
		log.info("Loading calendar.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.CALENDAR.fileName));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.CALENDAR.columns);

		// assuming all days really do follow monday in the file
		int indexMonday = col.get("monday");

		String[] line = reader.readNext();
		while(line != null) {
			boolean[] days = new boolean[7];
			for(int d = indexMonday; d < days.length; d++) {
				days[d] = line[d].equals("1");
			}
			services.put(line[col.get("service_id")], new Service(days, line[col.get("start_date")], line[col.get("end_date")]));

			line = reader.readNext();
		}

		reader.close();
		log.info("...     calendar.txt loaded");
	}

	/**
	 * Adds service exceptions to {@link #services}
	 * <p/>
	 * <br/><br/>
	 * calendar_dates.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Exceptions for the service IDs defined in the calendar.txt file. If calendar_dates.txt includes ALL
	 * dates of service, this file may be specified instead of calendar.txt.
	 *
	 * @throws IOException
	 */
	private void loadCalendarDates() throws IOException {
		log.info("Loading calendar_dates.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.CALENDAR_DATES.fileName));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.CALENDAR_DATES.columns);

		String[] line = reader.readNext();
		while(line != null) {
			Service actual = services.get(line[col.get("service_id")]);
			if(line[col.get("exception_type")].equals("2"))
				actual.addException(line[col.get("date")]);
			else
				actual.addAddition(line[col.get("date")]);

			line = reader.readNext();
		}

		reader.close();
		log.info("...     calendar_dates.txt loaded");
	}

	/**
	 * Loads shapes (if available) and puts them in {@link #shapes}. A shape is a sequence of points, i.e. a line.
	 * <p/>
	 * <br/><br/>
	 * shapes.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Rules for drawing lines on a map to represent a transit organization's routes.
	 */
	private void loadShapes() {
		log.info("Looking for shapes.txt");
		// shapes are optional
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(root + GTFSDefinitions.SHAPES.fileName));

			String[] header = reader.readNext();
			Map<String, Integer> col = getIndices(header, GTFSDefinitions.SHAPES.columns);

			String[] line = reader.readNext();
			while(line != null) {
				usesShapes = true; // shape file might exists but could be empty

				Shape actual = shapes.get(line[col.get("shape_id")]);
				if(actual == null) {
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

	/**
	 * Basically just reads all routeIds and their corresponding names and types and puts them in {@link #gtfsRoutes}.
	 * <p/>
	 * <br/><br/>
	 * routes.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Transit routes. A route is a group of trips that are displayed to riders as a single service.
	 *
	 * @throws IOException
	 */
	private void loadRoutes() throws IOException {
		log.info("Loading routes.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.ROUTES.fileName));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.ROUTES.columns);

		String[] line = reader.readNext();
		int i = 0;
		while(line != null) {
			GTFSRoute newGtfsRoute = new GTFSRoute(line[col.get("route_id")], line[col.get("route_short_name")], GTFSDefinitions.RouteTypes.values()[Integer.parseInt(line[col.get("route_type")])]);
			gtfsRoutes.put(line[col.get("route_id")], newGtfsRoute);

			line = reader.readNext();
		}
		reader.close();
		log.info("...     gtfsRoutes.txt loaded");
	}

	/**
	 * Generates a trip with trip_id and adds it to the corresponding route (referenced by route_id) in {@link #gtfsRoutes}.
	 * Adds the shape_id as well (if shapes are used). Each trip uses one service_id, the serviceIds statistics are increased accordingly
	 * <p/>
	 * <br/><br/>
	 * trips.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Trips for each route. A trip is a sequence of two or more gtfsStops that occurs at specific time.
	 *
	 * @throws IOException
	 */
	private void loadTrips() throws IOException {
		log.info("Loading trips.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.TRIPS.fileName));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.TRIPS.columns);

		String[] line = reader.readNext();
		while(line != null) {
			GTFSRoute GTFSRoute = gtfsRoutes.get(line[col.get("route_id")]);
			if(usesShapes) {
				Trip newTrip = new Trip(line[col.get("trip_id")], services.get(line[col.get("service_id")]), shapes.get(line[col.get("shape_id")]), line[col.get("trip_id")]);
				GTFSRoute.putTrip(line[col.get("trip_id")], newTrip);
			} else {
				Trip newTrip = new Trip(line[col.get("trip_id")], services.get(line[col.get("service_id")]), null, line[col.get("trip_id")]);
				GTFSRoute.putTrip(line[col.get("trip_id")], newTrip);
			}

			// each trip uses one service id, increase statistics accordingly
			if(serviceIdsAlgorithm.equals(SERVICE_ID_MOST_USED)) {
				Integer count = serviceIdsCount.get(line[col.get("service_id")]);
				if(count == null)
					serviceIdsCount.put(line[col.get("service_id")], 1);
				else
					serviceIdsCount.put(line[col.get("service_id")], count + 1);
			}
			line = reader.readNext();
		}

		reader.close();
		log.info("...     trips.txt loaded");
	}

	/**
	 * Stop times are added to their respective trip (which are stored in {@link #gtfsRoutes}).
	 * <p/>
	 * <br/><br/>
	 * stop_times.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Times that a vehicle arrives at and departs from individual gtfsStops for each trip.
	 *
	 * @throws IOException
	 */
	private void loadStopTimes() throws IOException {
		log.info("Loading stop_times.txt");
		CSVReader reader = new CSVReader(new FileReader(root + GTFSDefinitions.STOP_TIMES.fileName));
		String[] header = reader.readNext();
		Map<String, Integer> col = getIndices(header, GTFSDefinitions.STOP_TIMES.columns);

		String[] line = reader.readNext();
		int i = 1, c = 1;
		while(line != null) {
			if(i == Math.pow(2, c)) {
				log.info("        line #" + i);
				c++;
			}
			i++; // just for logging so something happens in the console

			for(GTFSRoute actualGTFSRoute : gtfsRoutes.values()) {
				Trip trip = actualGTFSRoute.getTrips().get(line[col.get("trip_id")]);
				if(trip != null) {
					try { // todo why try/catch?
						trip.putStopTime(
								Integer.parseInt(line[col.get("stop_sequence")]),
								new StopTime(Integer.parseInt(line[col.get("stop_sequence")]),
										timeFormat.parse(line[col.get("arrival_time")]),
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

	/**
	 * Loads the frequencies (if available) and adds them to their respective trips in {@link #gtfsRoutes}.
	 * <p/>
	 * <br/><br/>
	 * frequencies.txt <i>[https://developers.google.com/transit/gtfs/reference]</i><br/>
	 * Headway (time between trips) for gtfsRoutes with variable frequency of service.
	 */
	private void loadFrequencies() {
		log.info("Looking for frequencies.txt");
		// frequencies are optional
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(root + GTFSDefinitions.FREQUENCIES.fileName));
			String[] header = reader.readNext();
			Map<String, Integer> col = getIndices(header, GTFSDefinitions.FREQUENCIES.columns);

			String[] line = reader.readNext();
			while(line != null) {
				usesFrequencies = true;    // frequencies file might exists but could be empty

				for(GTFSRoute actualGTFSRoute : gtfsRoutes.values()) {
					Trip trip = actualGTFSRoute.getTrips().get(line[col.get("trip_id")]);
					if(trip != null) {
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
	 * In case optional columns in a csv file are missing or are out of order, adressing array
	 * values directly via integer (i.e. where the column should be) does not work.
	 *
	 * @param header      the header (first line) of the csv file
	 * @param columnNames array of attributes you need the indices of
	 * @return the index for each attribute given in columnNames
	 */
	public static Map<String, Integer> getIndices(String[] header, String[] columnNames) {
		Map<String, Integer> indices = new HashMap<>();

		for(String columnName : columnNames) {
			for(int i = 0; i < header.length; i++) {
				if(header[i].equals(columnName)) {
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
	 * sets the service id depending on the specified mode.
	 *
	 * @param serviceIdAlgorithm The algorithm with which you want to get the service ids.
	 *                           Currently only <i>mostused</i> and <i>all</i> are implemented.
	 */
	private void setServiceIds(String serviceIdAlgorithm) {

		if(serviceIdAlgorithm.equals(SERVICE_ID_MOST_USED)) {
			serviceIds = new String[1];
			serviceIds[0] = getKeyOfMaxValue(serviceIdsCount);

			log.info("        Getting most used service ID: " + serviceIds[0] + " (" + serviceIdsCount.get(serviceIds[0]) + " occurences)");
		} else {
			log.warn("Using all service IDs (probably way too much data)");

			int i = 0;
			serviceIds = new String[services.size()];
			for(String s : services.keySet()) {
				serviceIds[i] = s;
				i++;
			}
		}
	}

	/**
	 * Identifies the most used sevice ID of the validity period of the schedule and returns the ID.
	 *
	 * @author polettif
	 */
	private static String getKeyOfMaxValue(Map<String, Integer> map) {
		return map.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	}

	/**
	 * helper class for meaningful departureIds
	 */
	private class DepartureIds {

		private Map<Id<TransitRoute>, Integer> ids = new HashMap<>();

		public String getNext(Id<TransitRoute> transitRouteId) {
			if(!ids.containsKey(transitRouteId)) {
				ids.put(transitRouteId, 1);
				return transitRouteId + "_01";
			} else {
				int i = ids.put(transitRouteId, ids.get(transitRouteId) + 1) + 1;
				return transitRouteId + "_" + String.format("%03d", i);
			}

		}
	}
}