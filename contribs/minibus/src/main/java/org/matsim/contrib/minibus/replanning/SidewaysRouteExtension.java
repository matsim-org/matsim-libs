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

package org.matsim.contrib.minibus.replanning;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

/**
 * Takes the route transformed into a lineString to calculate a buffer around it. 
 * Chooses then randomly a new stop within the buffer and inserts it in both directions of the line.
 * This is done assuming the most distant stops to be the termini.
 * 
 * @author aneumann
 *
 */
public final class SidewaysRouteExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(SidewaysRouteExtension.class);
	public static final String STRATEGY_NAME = "SidewaysRouteExtension";
	private final double bufferSize;
	private final double ratio;
	private final boolean excludeTermini;
	
	public SidewaysRouteExtension(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 3){
			log.error("Parameter 1: Buffer size in meter");
			log.error("Parameter 2: Ratio bufferSize to route's beeline length. If set to something very small, e.g. 0.01, the calculated buffer size may be smaller than the one specified in parameter 1. Parameter 1 will then be taken as minimal buffer size.");
			log.error("Parameter 3: Remove buffer from termini - true/false");
		}
		this.bufferSize = Double.parseDouble(parameter.get(0));
		this.ratio = Double.parseDouble(parameter.get(1));
		this.excludeTermini = Boolean.parseBoolean(parameter.get(2));
	}

	@Override
	public PPlan run(Operator operator) {

		PPlan oldPlan = operator.getBestPlan();
		ArrayList<TransitStopFacility> currentStopsToBeServed = oldPlan.getStopsToBeServed();
		
		TransitStopFacility baseStop = currentStopsToBeServed.get(0);
		TransitStopFacility remoteStop = this.findStopWithLargestDistance(currentStopsToBeServed);
		double bufferSizeBasedOnRatio = CoordUtils.calcEuclideanDistance(baseStop.getCoord(), remoteStop.getCoord()) * this.ratio;
		
		List<Geometry> lineStrings = this.createGeometryFromStops(currentStopsToBeServed, remoteStop);
		Geometry buffer = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), this.excludeTermini);
		
		Set<Id<TransitStopFacility>> stopsUsed = this.getStopsUsed(oldPlan.getLine().getRoutes().values());
		TransitStopFacility newStop = this.drawRandomStop(buffer, operator.getRouteProvider(), stopsUsed);
		
		if (newStop == null) {
			return null;
		}
		
		ArrayList<TransitStopFacility> newStopsToBeServed = this.addStopToExistingStops(baseStop, remoteStop, currentStopsToBeServed, newStop);
		
		// create new plan
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), oldPlan.getId());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(oldPlan.getStartTime());
		newPlan.setEndTime(oldPlan.getEndTime());
		newPlan.setStopsToBeServed(newStopsToBeServed);
		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}


	private ArrayList<TransitStopFacility> addStopToExistingStops(TransitStopFacility baseStop, TransitStopFacility remoteStop, ArrayList<TransitStopFacility> currentStopsToBeServed, TransitStopFacility newStop) {
		List<TransitStopFacility> markerStops = new LinkedList<>();
		markerStops.add(baseStop);
		markerStops.add(remoteStop);
		
		List<ArrayList<TransitStopFacility>> subRoutes = this.cutRouteIntoSubRoutes(markerStops, currentStopsToBeServed);
		for (ArrayList<TransitStopFacility> subRoute : subRoutes) {
			this.addNewStopBetweenTwoNearestExistingStops(subRoute, newStop);
		}
		
		// merger sub routes
		ArrayList<TransitStopFacility> newStopsToBeServed = new ArrayList<>();
		for (ArrayList<TransitStopFacility> subRoute : subRoutes) {
			for (TransitStopFacility stop : subRoute) {
				newStopsToBeServed.add(stop);
			}
		}
		return newStopsToBeServed;
	}


	private void addNewStopBetweenTwoNearestExistingStops(ArrayList<TransitStopFacility> stops, TransitStopFacility stop) {
		// find nearest stop
		double smallestDistance = Double.MAX_VALUE;
		TransitStopFacility stopWithSmallestDistance = stops.get(0);
		TransitStopFacility stopWithSecondSmallestDistance = stops.get(0);
		for (TransitStopFacility transitStopFacility : stops) {
			double currentDistance = CoordUtils.calcEuclideanDistance(stop.getCoord(), transitStopFacility.getCoord());
			if (currentDistance < smallestDistance) {
				smallestDistance = currentDistance;
				stopWithSecondSmallestDistance = stopWithSmallestDistance;
				stopWithSmallestDistance = transitStopFacility;
			}
		}
		
		// find index to insert
		int index = Math.min(stops.indexOf(stopWithSecondSmallestDistance), stops.indexOf(stopWithSmallestDistance));
		
		// insert
		stops.add(index + 1, stop);
	}

	private List<ArrayList<TransitStopFacility>> cutRouteIntoSubRoutes(List<TransitStopFacility> markerStops, ArrayList<TransitStopFacility> stops) {
		List<ArrayList<TransitStopFacility>> subRoutes = null;
		ArrayList<TransitStopFacility> subRoute = null;
		
		for (TransitStopFacility stop : stops) {
			if (markerStops.contains(stop)) {
				if (subRoutes == null) {
					// very first sub route
					subRoutes = new LinkedList<>();
					subRoute = new ArrayList<>();
					subRoute.add(stop);
					subRoutes.add(subRoute);
				} else {
					// create new one
					subRoute = new ArrayList<>();
					subRoutes.add(subRoute);
					subRoute.add(stop);
				}
			} else {
				subRoute.add(stop);
			}
		}
		
		return subRoutes;
	}

	private TransitStopFacility findStopWithLargestDistance(ArrayList<TransitStopFacility> stops) {
		Coord startCoord = stops.get(0).getCoord();
		double largestDistance = 0;
		TransitStopFacility stopWithLargestDistance = stops.get(0);
		for (TransitStopFacility transitStopFacility : stops) {
			double currentDistance = CoordUtils.calcEuclideanDistance(startCoord, transitStopFacility.getCoord());
			if (currentDistance > largestDistance) {
				largestDistance = currentDistance;
				stopWithLargestDistance = transitStopFacility;
			}
		}
		return stopWithLargestDistance;
	}

	private Set<Id<TransitStopFacility>> getStopsUsed(Collection<TransitRoute> routes) {
		Set<Id<TransitStopFacility>> stopsUsed = new TreeSet<>();
		for (TransitRoute route : routes) {
			for (TransitRouteStop stop : route.getStops()) {
				stopsUsed.add(stop.getStopFacility().getId());
			}
		}
		return stopsUsed;
	}

	private TransitStopFacility drawRandomStop(Geometry buffer, PRouteProvider pRouteProvider, Set<Id<TransitStopFacility>> stopsUsed) {
		List<TransitStopFacility> choiceSet = new LinkedList<>();
		
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
		List<Geometry> geometries = new LinkedList<>();
		
		ArrayList<Coordinate> coords = new ArrayList<>();
		for (TransitStopFacility stop : stops) {
			if (stop.equals(remoteStop)) {
				// terminate current line string
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
				Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
				Geometry lineString = new GeometryFactory().createLineString(coordinates);
				geometries.add(lineString);
				// create new line string
				coords = new ArrayList<>();
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
	public String getStrategyName() {
		return SidewaysRouteExtension.STRATEGY_NAME;
	}
}
