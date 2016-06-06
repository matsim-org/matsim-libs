/* *********************************************************************** *
 * project: org.matsim.*
 * GTFSDefinitions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs.lib;

public final class GTFSDefinitions {

	// column names
	public static final String SHAPE_ID = "shape_id";
	public static final String SHAPE_PT_LON = "shape_pt_lon";
	public static final String SHAPE_PT_LAT = "shape_pt_lat";
	public static final String SHAPE_PT_SEQUENCE = "shape_pt_sequence";
	public static final String STOP_LON = "stop_lon";
	public static final String STOP_LAT = "stop_lat";
	public static final String STOP_NAME = "stop_name";
	public static final String STOP_ID = "stop_id";
	public static final String SERVICE_ID = "service_id";
	public static final String START_DATE = "start_date";
	public static final String END_DATE = "end_date";
	public static final String EXCEPTION_TYPE = "exception_type";
	public static final String DATE = "date";
	public static final String ROUTE_SHORT_NAME = "route_short_name";
	public static final String ROUTE_TYPE = "route_type";
	public static final String ROUTE_ID = "route_id";
	public static final String TRIP_ID = "trip_id";
	public static final String STOP_SEQUENCE = "stop_sequence";
	public static final String ARRIVAL_TIME = "arrival_time";
	public static final String DEPARTURE_TIME = "departure_time";
	public static final String START_TIME = "start_time";
	public static final String END_TIME = "end_time";
	public static final String HEADWAY_SECS = "headway_secs";
	public static final String MONDAY = "monday";

	//Constants
	/**
	 * Values
	 */
	public enum Files {
		STOPS("Stop", "stops.txt", new String[]{STOP_ID, STOP_LON, STOP_LAT, STOP_NAME}),
		CALENDAR("Calendar", "calendar.txt", new String[]{SERVICE_ID, MONDAY, START_DATE, END_DATE}),
		CALENDAR_DATES("CalendarDates", "calendar_dates.txt", new String[]{SERVICE_ID, DATE, EXCEPTION_TYPE}),
		SHAPES("Shape", "shapes.txt", new String[]{SHAPE_ID, SHAPE_PT_LON, SHAPE_PT_LAT, SHAPE_PT_SEQUENCE}),
		ROUTES("Route", "routes.txt", new String[]{ROUTE_ID, ROUTE_SHORT_NAME, ROUTE_TYPE}),
		TRIPS("Trip", "trips.txt", new String[]{ROUTE_ID, TRIP_ID, SERVICE_ID, SHAPE_ID}),
		STOP_TIMES("StopTime", "stop_times.txt", new String[]{TRIP_ID, STOP_SEQUENCE, ARRIVAL_TIME, DEPARTURE_TIME, STOP_ID}),
		FREQUENCIES("Frequency", "frequencies.txt", new String[]{TRIP_ID, START_TIME, END_TIME, HEADWAY_SECS});

		//Attributes
		public String name;
		public String fileName;
		public String[] columns;

		//Methods
		Files(String name, String fileName, String[] columns) {
			this.name = name;
			this.fileName = fileName;
			this.columns = columns;
		}
	}


	
	public enum WayTypes {
		RAIL,
		ROAD,
		WATER,
		CABLE
	}
	public enum RouteTypes {
		//Values
		TRAM("tram",WayTypes.RAIL),
		SUBWAY("subway",WayTypes.RAIL),
		RAIL("rail",WayTypes.RAIL),
		BUS("bus",WayTypes.ROAD),
		FERRY("ferry",WayTypes.WATER),
		CABLE_CAR("cable car",WayTypes.CABLE),
		GONDOLA("gondola",WayTypes.CABLE),
		FUNICULAR("funicular",WayTypes.RAIL);
		//Attributes
		public String name;
		public WayTypes wayType;
		//Methods
		RouteTypes(String name,WayTypes wayType) {
			this.name = name;
			this.wayType = wayType;
		}
	}

}
