/* *********************************************************************** *
 * project: org.matsim.*
 * VisumNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mohit.converter;


import java.util.HashMap;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.map.LinkedMap;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;


public class VisumNetwork {

	public final Map<Id, Stop> stops = new TreeMap<Id, Stop>();
	public final Map<Id, StopArea> stopAreas = new TreeMap<Id, StopArea>();
	public final Map<String, StopPoint> stopPoints = new TreeMap<String, StopPoint>();
	public final Map<Id, TransitLine> lines = new TreeMap<Id, TransitLine>();
	public final Map<String, TransitLineRoute> lineRoutes = new TreeMap<String, TransitLineRoute>();
	public final Map<String , LineRouteItem> lineRouteItems = new TreeMap<String, LineRouteItem>();
	public final Map<String , TimeProfile> timeProfiles = new TreeMap<String, TimeProfile>();
	public final Map<String , TimeProfileItem> timeProfileItems = new LinkedMap();
	public final Map<String , Departure> departures = new TreeMap<String, Departure>();
    public final Map<String, TransportMode> transportModes = new HashMap<String, TransportMode>();
	
   
    public void addStop(final Stop stop) {
		Stop oldStop = this.stops.put(stop.id, stop);
		if (oldStop != null) {
			// there was already a stop with the same id
			// re-do the insertion
			this.stops.put(oldStop.id, oldStop);
			throw new IllegalArgumentException("There is already a stop with the same Name");
		}
	
	}
    public void addStopArea(final StopArea stopAr) {
		StopArea oldStopAr = this.stopAreas.put(stopAr.id, stopAr);
		if (oldStopAr != null) {
			// there was already a stop point with the same id
			// re-do the insertion
			this.stopAreas.put(oldStopAr.id, oldStopAr);
			throw new IllegalArgumentException("There is already a stop area with the same id.");
		}
	
	}

	public void addStopPoint(final StopPoint stopPt) {
		StopPoint oldStopPt = this.stopPoints.put(stopPt.stopAreaId.toString()+stopPt.id.toString(), stopPt);
		if (oldStopPt != null) {
			// there was already a stop point with the same id
			// re-do the insertion
			this.stopPoints.put(oldStopPt.stopAreaId.toString()+oldStopPt.id.toString(), oldStopPt);
			throw new IllegalArgumentException("There is already a stop with the same id.");
		}
	
	}
	public void addline(final TransitLine l1) {
		TransitLine oldl = this.lines.put(l1.id, l1);
		if (oldl != null) {
			// there was already a line with the same id
			// re-do the insertion
			this.lines.put(oldl.id, oldl);
			throw new IllegalArgumentException("There is already a line with the same id.");
		}
		
	}
	public void addLineRoute(final TransitLineRoute lr1) {
		TransitLineRoute oldlr = this.lineRoutes.put(lr1.lineName.toString()+"/"+lr1.id.toString()+"/"+lr1.DCode.toString(), lr1);
		if (oldlr != null) {
			// there was already a line route with the same id
			// re-do the insertion
			this.lineRoutes.put(oldlr.lineName.toString()+"/"+oldlr.id.toString()+"/"+oldlr.DCode.toString(), oldlr);
			throw new IllegalArgumentException("There is already a line route with the same id.");
		}
		
	}
	public void addLineRouteItem(final LineRouteItem lri1) {
		LineRouteItem oldlri = this.lineRouteItems.put(lri1.lineName +"/"+ lri1.lineRouteName +"/"+lri1.index+"/"+lri1.DCode, lri1);
		if (oldlri != null) {
			// there was already a Line Route Item with the same id
			// re-do the insertion
			this.lineRouteItems.put(oldlri.lineName +"/"+oldlri.lineRouteName+"/"+oldlri.index+"/"+oldlri.DCode, oldlri);
		throw new IllegalArgumentException("There is already a route item with the same id."+oldlri.lineName +"/"+ oldlri.lineRouteName +"/"+oldlri.index+"/"+ oldlri.DCode);
		}
		
	}
	public void addTimeProfile(final TimeProfile tp1) {
		TimeProfile oldtp = this.timeProfiles.put(tp1.lineName.toString()+"/"+tp1.lineRouteName.toString() +"/"+tp1.DCode.toString()+"/"+ tp1.index.toString(), tp1);
		if (oldtp != null) {
			// there was already a stop with the same id
			// re-do the insertion
			this.timeProfiles.put(oldtp.lineName.toString()+"/"+oldtp.lineRouteName.toString() +"/"+oldtp.DCode.toString()+"/"+ oldtp.index.toString(), oldtp);
		throw new IllegalArgumentException("There is already a time profile with the same id."+oldtp.lineRouteName.toString() +"/"+ oldtp.index.toString());
		}
		
	}
	public void addTimeProfileItem(final TimeProfileItem tpi1) {
		TimeProfileItem oldtpi = this.timeProfileItems.put(tpi1.lineName+"/"+tpi1.lineRouteName+"/"+tpi1.timeProfileName+"/"+tpi1.DCode+"/"+tpi1.index, tpi1);
		if (oldtpi != null) {
			// there was already a stop with the same id
			// re-do the insertion
			this.timeProfileItems.put(oldtpi.lineName+"/"+oldtpi.lineRouteName+"/"+oldtpi.timeProfileName+"/"+oldtpi.DCode+"/"+oldtpi.index, oldtpi);
		throw new IllegalArgumentException("There is already a time profile item with the same id.");
		}
		
	}
	public void addDeparture(final Departure d) {
		Departure oldD = this.departures.put(d.lineName +"/"+ d.lineRouteName +"/"+ d.index, d);
		if (oldD != null) {
			// there was already a stop with the same id
			// re-do the insertion
			this.departures.put(oldD.lineName+"/"+ oldD.lineRouteName +"/"+ oldD.index, oldD);
		throw new IllegalArgumentException("There is already a departure with the same id.");
		}
		
	}

	
	public static class Stop {
		public final Id id;
		public final String name;
		public final Coord coord;

		public Stop(final Id id, final String name, final Coord coord) {
			this.id = id;
			this.name = name;
			this.coord = coord;
		}
	}
	public static class StopArea {
		public final Id id;
		public final Id StopId;
		

		public StopArea(final Id id, final Id StopId) {
			this.id = id;
			this.StopId = StopId;
		
		}
	}
	public static class StopPoint {
		public final Id id,stopAreaId;
		
		public final String name;
		public final Id refLinkNo;

		public StopPoint(final Id id, final Id stopAreaId, final String name, final Id refLinkNo) {
			this.id = id;
			this.stopAreaId = stopAreaId;
			this.name = name;
			this.refLinkNo = refLinkNo;
		}
	}
	public static class TransitLineRoute {

		public final Id id;
		public final Id lineName;
		public final Id DCode;
		
		

		public TransitLineRoute(final Id id,final Id lineName,final Id DCode) {
		this.id = id;;
		this.lineName = lineName;
		this.DCode = DCode;
		}
	}
	
	public static class TransitLine {

		public final Id id;
		public final String tCode;
		

		public TransitLine(final Id id, final String tCode) {
			this.id = id;
			this.tCode = tCode;
		}
	}

	public static class LineRouteItem {
		public final String lineName;
		public final String lineRouteName;
		public final String index;
		public final String DCode;
		public final Id stopPointNo;
		
		

		public LineRouteItem(final String lineName, final String lineRouteName, final String index,final String DCode, final Id stopPointNo) {
			this.lineName = lineName;
			this.lineRouteName = lineRouteName;
			this.index = index;
			this.stopPointNo = stopPointNo;
			this.DCode = DCode;
		}
	}
	
	public static class TimeProfile {
		public final Id lineName;
		public final Id lineRouteName;
		public final Id index;
		public final Id DCode;
		
		

		public TimeProfile(final Id lineName, final Id lineRouteName, final Id index,final Id DCode) {
			this.lineName = lineName;
			this.lineRouteName = lineRouteName;
			this.index = index;
			this.DCode = DCode;
		}
	}
	
	public static class TimeProfileItem {
		public final String lineName;
		public final String lineRouteName;
		public final String timeProfileName;
		public final String index;
		public final String arr;
		public final String dep;
		public final Id lRIIndex;
		public final String DCode;

		public TimeProfileItem(final String lineName, final String lineRouteName,  final String timeProfileName,final String DCode,final String index, final String arr, final String dep, final Id lRIIndex) {
			this.lineName = lineName;
			this.lineRouteName = lineRouteName;
			this.timeProfileName = timeProfileName;
			this.DCode = DCode;
			this.index = index;
			this.arr = arr;
			this.dep = dep;
			this.lRIIndex = lRIIndex;
		}
	}
	public static class Departure {
		public final String lineName;
		public final String lineRouteName;
		public final String index;
		public final String TRI;
		public final String dep;
		public final Id DCode;
		
		

		public Departure(final String lineName, final String lineRouteName, final String index, final String TRI, final String dep,final Id DCode) {
			this.lineName = lineName;
			this.lineRouteName = lineRouteName;
			this.index = index;
			this.TRI = TRI;
			this.dep = dep;
			this.DCode=DCode;
			
		}
	}
}

		