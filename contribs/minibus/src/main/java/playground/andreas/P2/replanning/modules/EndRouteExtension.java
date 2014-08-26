/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.routeProvider.PRouteProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Takes the route transformed into a lineString to calculate a buffer around it.
 * Exludes the part of the buffer going parallel/along the actual route, so only the end caps remain.
 * Chooses then randomly a new stop within the buffer and inserts it after the nearest existing stop.
 * 
 * @author aneumann
 *
 */
public class EndRouteExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(EndRouteExtension.class);
	public static final String STRATEGY_NAME = "EndRouteExtension";
	private final double bufferSize;
	private final double ratio;
	
	public EndRouteExtension(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 2){
			log.error("Parameter 1: Buffer size in meter");
			log.error("Parameter 2: Ratio bufferSize to route's beeline length. If set to something very small, e.g. 0.01, the calculated buffer size may be smaller than the one specified in parameter 1. Parameter 1 will then be taken as minimal buffer size.");
		}
		this.bufferSize = Double.parseDouble(parameter.get(0));
		this.ratio = Double.parseDouble(parameter.get(1));
	}

	@Override
	public PPlan run(Cooperative cooperative) {

		PPlan oldPlan = cooperative.getBestPlan();
		ArrayList<TransitStopFacility> currentStopsToBeServed = oldPlan.getStopsToBeServed();
		
		TransitStopFacility baseStop = currentStopsToBeServed.get(0);
		TransitStopFacility remoteStop = this.findStopWithLargestDistance(currentStopsToBeServed);
		double bufferSizeBasedOnRatio = CoordUtils.calcDistance(baseStop.getCoord(), remoteStop.getCoord()) * this.ratio;
		
		List<Geometry> lineStrings = this.createGeometryFromStops(currentStopsToBeServed, remoteStop);
		Geometry bufferWithoutEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), true);
		Geometry bufferWithEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), false);
		Geometry buffer = bufferWithEndCaps.difference(bufferWithoutEndCaps);
		
		Set<Id> stopsUsed = this.getStopsUsed(oldPlan.getLine().getRoutes().values());
		TransitStopFacility newStop = this.drawRandomStop(buffer, cooperative.getRouteProvider(), stopsUsed);
		
		if (newStop == null) {
			return null;
		}
		
		ArrayList<TransitStopFacility> newStopsToBeServed = this.addStopToExistingStops(baseStop, remoteStop, currentStopsToBeServed, newStop);
		
		// create new plan
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(oldPlan.getStartTime());
		newPlan.setEndTime(oldPlan.getEndTime());
		newPlan.setStopsToBeServed(newStopsToBeServed);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}


	private ArrayList<TransitStopFacility> addStopToExistingStops(TransitStopFacility baseStop, TransitStopFacility remoteStop, ArrayList<TransitStopFacility> currentStopsToBeServed, TransitStopFacility newStop) {
		ArrayList<TransitStopFacility> newStopsToBeServed = new ArrayList<TransitStopFacility>(currentStopsToBeServed);
		
		// decide which stop is closer
		if (CoordUtils.calcDistance(baseStop.getCoord(), newStop.getCoord()) < CoordUtils.calcDistance(remoteStop.getCoord(), newStop.getCoord())) {
			// baseStop is closer - insert before baseStop
			newStopsToBeServed.add(0, newStop);
		} else {
			// remote stop is closer or both have the same distance - add after remote stop
			newStopsToBeServed.add(newStopsToBeServed.indexOf(remoteStop) + 1, newStop);
		}
		
		return newStopsToBeServed;
	}

	private TransitStopFacility findStopWithLargestDistance(ArrayList<TransitStopFacility> stops) {
		Coord startCoord = stops.get(0).getCoord();
		double largestDistance = 0;
		TransitStopFacility stopWithLargestDistance = stops.get(0);
		for (TransitStopFacility transitStopFacility : stops) {
			double currentDistance = CoordUtils.calcDistance(startCoord, transitStopFacility.getCoord());
			if (currentDistance > largestDistance) {
				largestDistance = currentDistance;
				stopWithLargestDistance = transitStopFacility;
			}
		}
		return stopWithLargestDistance;
	}

	private Set<Id> getStopsUsed(Collection<TransitRoute> routes) {
		Set<Id> stopsUsed = new TreeSet<Id>();
		for (TransitRoute route : routes) {
			for (TransitRouteStop stop : route.getStops()) {
				stopsUsed.add(stop.getStopFacility().getId());
			}
		}
		return stopsUsed;
	}

	private TransitStopFacility drawRandomStop(Geometry buffer, PRouteProvider pRouteProvider, Set<Id> stopsUsed) {
		List<TransitStopFacility> choiceSet = new LinkedList<TransitStopFacility>();
		
		// find choice-set
		for (TransitStopFacility stop : pRouteProvider.getAllPStops()) {
			if (!stopsUsed.contains(stop.getId())) {
				if (buffer.contains(MGC.coord2Point(stop.getCoord()))) {
					choiceSet.add(stop);
				}
			}
		}
		
		return pRouteProvider.drawRandomStopFromList(choiceSet);
	}


	private Geometry createBuffer(List<Geometry> lineStrings, double bufferSize, boolean excludeTermini) {
		BufferParameters bufferParameters = new BufferParameters();
		
		if (excludeTermini) {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		} else {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
		}
		
		Geometry union = null;
		
		for (Geometry lineString : lineStrings) {
			Geometry buffer = BufferOp.bufferOp(lineString, bufferSize, bufferParameters);
			if (union == null) {
				union = buffer;
			} else {
				union = union.union(buffer);
			}
		}
		
		return union;
	}

	private List<Geometry> createGeometryFromStops(ArrayList<TransitStopFacility> stops, TransitStopFacility remoteStop) {
		List<Geometry> geometries = new LinkedList<Geometry>();
		
		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		for (TransitStopFacility stop : stops) {
			if (stop.equals(remoteStop)) {
				// terminate current line string
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
				Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
				Geometry lineString = new GeometryFactory().createLineString(coordinates);
				geometries.add(lineString);
				// create new line string
				coords = new ArrayList<Coordinate>();
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
			} else {
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
			}
		}
		// add first stop to close the circle
		coords.add(new Coordinate(stops.get(0).getCoord().getX(), stops.get(0).getCoord().getY(), 0.0));
		
		Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
		Geometry lineString = new GeometryFactory().createLineString(coordinates);
		geometries.add(lineString);
		return geometries;
	}


	@Override
	public String getName() {
		return EndRouteExtension.STRATEGY_NAME;
	}
}
