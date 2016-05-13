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


package playground.polettif.publicTransitMapping.gtfs;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.publicTransitMapping.gtfs.containers.*;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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

	// todo await departure time?
	private boolean defaultAwaitDepartureTime = true;

	/**
	 * which algorithm should be used to get serviceIds
	 */
	private final String serviceIdsAlgorithm;
	public static final String ALL_SERVICE_IDS = "all";
	public static final String MOST_USED_SINGLE_ID = "mostUsedSingleId";
	public static final String DAY_WITH_MOST_TRIPS = "dayWithMostTrips";
	public static final String DAY_WITH_MOST_SERVICES = "dayWithMostServices";

	/**
	 * Path to the folder where the gtfs files are located
	 */
	private final String root;
	private CoordinateTransformation transformation = new IdentityTransformation();

	/**
	 * The types of dates that will be represented by the new file
	 */
	private Set<String> serviceIds = new HashSet<>();

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
	private Collection<SimpleFeature> features = new ArrayList<>();
	private TransitSchedule schedule;
	private TransitScheduleFactory scheduleFactory;


	/**
	 * Calls {@link #convertGTFS2MATSimTransitScheduleFile}.
	 */
	public static void main(final String[] args) {
		convertGTFS2MATSimTransitScheduleFile(args[0], args[1], args[2], args[3]);
	}

	/**
	 * Reads gtfs files in and converts them to an unmapped MATSim Transit Schedule (mts).
	 * "Unmapped" means stopFacilities are not referenced to links and transit routes do not have routes (link sequences).
	 * <p/>
	 *
	 * @param gtfsInputPath          folder where the gtfs files are located (a single zip file is not supported)
	 * @param serviceIdsParam		 which service ids should be used. One of the following:
	 *                               <ul>
	 *                               <li>date in the format "yyyymmdd"
	 *                               <li>"dayWithMostTrips"</li>
	 *                               <li>"dayWithMostServices"</li>
	 *                               <li>"mostUsedSingleId"</li>
	 *                               <li>"all"</li>
	 *                               </li>
	 *                               </ul>
	 * @param outputCoordinateSystem the output coordinate system. WGS84/identity transformation is used if <code>null</code>.
	 * @param mtsOutputFile          path to the (to be generated) unmapped transit schedule file
	 */
	public static void convertGTFS2MATSimTransitScheduleFile(String gtfsInputPath, String serviceIdsParam, String outputCoordinateSystem, String mtsOutputFile) {
		GTFSReader gtfsReader = new GTFSReader(gtfsInputPath, serviceIdsParam, outputCoordinateSystem);

		ScheduleTools.writeTransitSchedule(gtfsReader.getSchedule(), mtsOutputFile);
	}

	public static void convertWithShapes(String gtfsInputPath, String serviceIdsParam, String outputCoordinateSystem, String mtsOutputFile, String outputShapeFile) {
		GTFSReader gtfsReader = new GTFSReader(gtfsInputPath, serviceIdsParam, outputCoordinateSystem);

		gtfsReader.writeShapeFile(outputShapeFile);
		ScheduleTools.writeTransitSchedule(gtfsReader.getSchedule(), mtsOutputFile);
	}

	/**
	 * Constructor.
	 * @param gtfsInputPath          folder where the gtfs files are located (a single zip file is not supported)
	 * @param serviceIdsParam		 which service ids should be used. One of the following:
	 *                               <ul>
	 *                               <li>date in the format "yyyymmdd"
	 *                               <li>"dayWithMostTrips"</li>
	 *                               <li>"dayWithMostServices"</li>
	 *                               <li>"mostUsedSingleId"</li>
	 *                               <li>"all"</li>
	 *                               </li>
	 *                               </ul>
	 * @param outputCoordinateSystem the output coordinate system. WGS84/identity transformation is used if <code>null</code>.
	 */
	public GTFSReader(String gtfsInputPath, String serviceIdsParam, String outputCoordinateSystem) {
		Service.dateStats.clear();
		this.schedule = ScheduleTools.createSchedule();
		this.scheduleFactory = schedule.getFactory();

		this.root = gtfsInputPath;
		this.serviceIdsAlgorithm = serviceIdsParam;
		if(outputCoordinateSystem != null) {
			this.transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outputCoordinateSystem);
		}
		loadFiles();
		convert();
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

		log.info("Converting to MATSim transit schedule");

		setServiceIds(serviceIdsAlgorithm);

		int counterLines = 0;
		int counterRoutes = 0;

		/** [1]
		 * generating transitStopFacilities (mts) from gtfsStops and add them to the schedule.
		 * Coordinates are transformed here.
		 */
		for(Entry<String, GTFSStop> stopEntry : gtfsStops.entrySet()) {
			Coord result = transformation.transform(stopEntry.getValue().getPoint());
			TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(Id.create(stopEntry.getKey(), TransitStopFacility.class), result, stopEntry.getValue().isBlocks());
			stopFacility.setName(stopEntry.getValue().getName());
			schedule.addStopFacility(stopFacility);
		}

		if(usesFrequencies) {
			log.info("    Using frequencies.txt to generate departures");
		} else {
			log.info("    Using stop_times.txt to generate departures");
		}

		DepartureIds departureIds = new DepartureIds();

		for(GTFSRoute gtfsRoute : gtfsRoutes.values()) {

			/** [2]
			 * Create a MTS transitLine for each GTFSRoute
			 */
			TransitLine transitLine = scheduleFactory.createTransitLine(Id.create(gtfsRoute.getShortName()+"_"+gtfsRoute.getRouteId(), TransitLine.class));
			schedule.addTransitLine(transitLine);
			counterLines++;

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
						TransitRouteStop newTRS = scheduleFactory.createTransitRouteStop(schedule.getFacilities().get(Id.create(stopTime.getStopId(), TransitStopFacility.class)), arrival, departure);
						newTRS.setAwaitDepartureTime(defaultAwaitDepartureTime);
						transitRouteStops.add(newTRS);
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
						counterRoutes++;
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
							counterRoutes++;
						}
					}
				}
			} // foreach trip
		} // foreach route

		log.info("    Created "+counterRoutes+" routes on "+counterLines+" lines.");
		log.info("... GTFS converted to an unmapped MATSIM Transit Schedule");
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
		int i = 1, c=1;
		while(line != null) {
			if(i == Math.pow(2, c)) { log.info("        # " + i); c++; } i++;

			boolean[] days = new boolean[7];
			for(int d = 0; d < 7; d++) {
				days[d] = line[indexMonday + d].equals("1");
			}
			services.put(line[col.get("service_id")], new Service(line[col.get("service_id")], days, line[col.get("start_date")], line[col.get("end_date")]));

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
			Service currentService = services.get(line[col.get("service_id")]);
			if(line[col.get("exception_type")].equals("2"))
				currentService.addException(line[col.get("date")]);
			else
				currentService.addAddition(line[col.get("date")]);

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

				Shape currentShape = shapes.get(line[col.get("shape_id")]);
				if(currentShape == null) {
					currentShape = new Shape(line[col.get("shape_id")]);
					shapes.put(line[col.get("shape_id")], currentShape);
				}
				currentShape.addPoint(new Coord(Double.parseDouble(line[col.get("shape_pt_lon")]), Double.parseDouble(line[col.get("shape_pt_lat")])), Integer.parseInt(line[col.get("shape_pt_sequence")]));
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
		log.info("...     routes.txt loaded");
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
			Integer count = MapUtils.getInteger(line[col.get("service_id")], serviceIdsCount, 1);
			serviceIdsCount.put(line[col.get("service_id")], count + 1);

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
			if(i == Math.pow(2, c)) { log.info("        # " + i); c++; }
			i++; // just for logging so something happens in the console

			for(GTFSRoute actualGTFSRoute : gtfsRoutes.values()) {
				Trip trip = actualGTFSRoute.getTrips().get(line[col.get("trip_id")]);
				if(trip != null) {
					try {
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
	 * @param param The date for which all service ids should be looked up.
	 *              Or the algorithm with which you want to get the service ids.
	 *              (Currently only <i>mostused</i> and <i>all</i> are implemented).
	 */
	private void setServiceIds(String param) {
		// todo doc param
		switch (param) {
			case MOST_USED_SINGLE_ID:
				String mostUsed = getKeyOfMaxValue(serviceIdsCount);
				serviceIds = Collections.singleton(mostUsed);
				log.info("... Getting most used service ID: " + mostUsed + " (" + serviceIdsCount.get(mostUsed) + " occurences)");
				break;

			case ALL_SERVICE_IDS:
				log.info("... Using all service IDs (probably way too much data)");
				serviceIds = services.keySet();
				break;

			case DAY_WITH_MOST_SERVICES: {
				LocalDate busiestDate = null;
				for(Entry<LocalDate, Set<String>> e : Service.dateStats.entrySet()) {
					if(e.getValue().size() > serviceIds.size()) {
						serviceIds = e.getValue();
						busiestDate = e.getKey();
					}
				}
				log.info("... Using service IDs of the day with the most services (" + DAY_WITH_MOST_SERVICES + ").");
				log.info("    " + serviceIds.size() + " services on " + busiestDate);
				break;
			}

			case DAY_WITH_MOST_TRIPS: {
				LocalDate busiestDate = null;
				int maxTrips = 0;
				for(Entry<LocalDate, Set<String>> e : Service.dateStats.entrySet()) {
					int nTrips = 0;
					for(String s : e.getValue()) {
						nTrips += serviceIdsCount.get(s);
					}
					if(nTrips > maxTrips) {
						maxTrips = nTrips;
						serviceIds = e.getValue();
						busiestDate = e.getKey();
					}
				}
				log.info("... Using service IDs of the day with the most trips (" + DAY_WITH_MOST_TRIPS + ").");
				log.info("    " + maxTrips + " trips and " + serviceIds.size() + " services on " + busiestDate);
				break;
			}

			default:
				LocalDate checkDate = LocalDate.of(Integer.parseInt(param.substring(0, 4)), Integer.parseInt(param.substring(4, 6)), Integer.parseInt(param.substring(6, 8)));

				serviceIds = getServiceIdsOnDate(checkDate);
				log.info("        Using service IDs on " + param + ": " + serviceIds.size() + " services.");
				break;
		}
	}

	/**
	 * Identifies the most used sevice ID of the validity period of the schedule and returns the ID.
	 */
	private static String getKeyOfMaxValue(Map<String, Integer> map) {
		return map.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	}

	public void setTransformation(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}

	public Set<String> getServiceIdsOnDate(LocalDate checkDate) {
		HashSet<String> idsOnCheckDate = new HashSet<>();
		for(Service service : services.values()) {
			if(dateIsOnService(checkDate, service)) {
				idsOnCheckDate.add(service.getId());
			}
		}
		return idsOnCheckDate;
	}

	/**
	 * @return <code>true</code> if the given date is used by the given service.
	 */
	private boolean dateIsOnService(LocalDate checkDate, Service service) {
		// check if checkDate is an addition
		if(service.getAdditions().contains(checkDate)) {
			return true;
		}
		if(checkDate.isBefore(service.getEndDate()) && checkDate.isAfter(service.getStartDate())) {
			// check if the checkDate is not an exception of the service
			if(service.getExceptions().contains(checkDate)) {
				return false;
			}
			// get weekday (0 = monday)
			int weekday = checkDate.getDayOfWeek().getValue() - 1;
			return service.getDays()[weekday];
		}
		return false;
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

	public void writeShapeFile(String outFile) {
		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("gtfs_shapes")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("id", String.class)
				.addAttribute("trip_id", String.class)
				.addAttribute("trip_name", String.class)
				.addAttribute("route_id", String.class)
				.addAttribute("route_name", String.class)
				.create();

		for(GTFSRoute gtfsRoute : gtfsRoutes.values()) {
			for(Trip trip : gtfsRoute.getTrips().values()) {
				boolean useTrip = false;
				for(String serviceId : serviceIds) {
					if(trip.getService().equals(services.get(serviceId))) {
						useTrip = true;
						break;
					}
				}

				if(useTrip) {
					Shape shape = trip.getShape();
					if(shape != null) {
						SimpleFeature f = ff.createPolyline(shape.getCoordinates());
						f.setAttribute("id", shape.getId());
						f.setAttribute("trip_id", trip.getId());
						f.setAttribute("trip_name", trip.getName());
						f.setAttribute("route_id", gtfsRoute.getRouteId());
						f.setAttribute("route_name", gtfsRoute.getShortName());
						features.add(f);
					}
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, outFile);
	}
}