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

package playground.polettif.publicTransitMapping.gtfs.containers;

public enum GTFSDefinitions {

	//Constants
	/**
	 * Values
	 */
	STOPS("Stop","stops.txt",new String[]{"stop_id","stop_lon","stop_lat","stop_name"}),
	CALENDAR("Calendar","calendar.txt",new String[]{"service_id","monday","start_date","end_date"}),
	CALENDAR_DATES("CalendarDates","calendar_dates.txt",new String[]{"service_id","date","exception_type"}),
	SHAPES("Shape","shapes.txt",new String[]{"shape_id","shape_pt_lon","shape_pt_lat","shape_pt_sequence"}),
	ROUTES("Route","routes.txt",new String[]{"route_id","route_short_name","route_type"}),
	TRIPS("Trip","trips.txt",new String[]{"route_id","trip_id","service_id","shape_id"}),
	STOP_TIMES("StopTime","stop_times.txt",new String[]{"trip_id","stop_sequence","arrival_time","departure_time","stop_id"}),
	FREQUENCIES("Frequency","frequencies.txt",new String[]{"trip_id","start_time","end_time","headway_secs"});

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
	
	//Attributes
	public String name;
	public String fileName;
	public String[] columns;
	
	//Methods
	GTFSDefinitions(String name, String fileName, String[] columns) {
		this.name = name;
		this.fileName = fileName;
		this.columns = columns;
	}
	
}
