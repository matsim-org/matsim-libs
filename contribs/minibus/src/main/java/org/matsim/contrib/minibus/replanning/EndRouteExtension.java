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
 * Excludes the part of the buffer going parallel/along the actual route, so only the end caps remain.
 * Chooses then randomly a new stop within the buffer and inserts it after the nearest existing stop.
 * 
 * @author aneumann
 *
 */
public final class EndRouteExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(EndRouteExtension.class);
	public static final String STRATEGY_NAME = "EndRouteExtension";
	private final double bufferSize;
	private final double ratio;
	
	public EndRouteExtension(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 2){
			log.error("Parameter 1: Buffer size in meter");
			log.error("Parameter 2: Ratio bufferSize to route's beeline length. If set to something very small, e.g. 0.01, the calculated buffer size may be smaller than the one specified in parameter 1. Parameter 1 will then be taken as minimal buffer size.");
		}
		this.bufferSize = Double.parseDouble(parameter.get(0));
		this.ratio = Double.parseDouble(parameter.get(1));
	}

	@Override
	public PPlan run(Operator operator) {

		PPlan oldPlan = operator.getBestPlan();
		ArrayList<TransitStopFacility> currentStopsToBeServed = oldPlan.getStopsToBeServed();
		
		TransitStopFacility baseStop = currentStopsToBeServed.get(0);
		/* 
		 * Paratransit TransitRoutes are always circular instead of having one TransitRoute each for each direction between two 
		 * terminus stops. So we only know one terminus stop, the base stop, and try to find the other terminus, the other end 
		 * of the route, by scanning the stops on the TransitRoute for the stop which has the largest distance from the base stop.
		 */
		int remoteStopIndex = this.findStopIndexWithLargestDistance(currentStopsToBeServed);
		
		if (baseStop.equals(currentStopsToBeServed.get(remoteStopIndex))) {
			/*
			 * TODO:
			 * findStopWithLargestDistance() should never return a remote stop which equals the base stop unless the input 
			 * stop sequence of the oldPlan has only stops at exactly the same coordinate (which should be prohibited somewhere
			 * else). If baseStop and remoteStop are equal createGeometryFromStops() fails because it asks for a LineString of 
			 * just one point (the base stop = remote stop).
			 */
			log.debug(
					"EndRouteExtension replanning is skipped for TransitLine "
							+ operator.getCurrentTransitLine().getId()
							+ ", because base stop and remote stop returned by findStopWithLargestDistance() are equal. This"
							+ " should not happen. Base stop: " + baseStop.getId() + ". Remote stop index: " + remoteStopIndex);
			return null;
		}

		double bufferSizeBasedOnRatio = CoordUtils.calcEuclideanDistance(baseStop.getCoord(), currentStopsToBeServed.get(remoteStopIndex).getCoord()) * this.ratio;
		
		List<Geometry> lineStrings = this.createGeometryFromStops(currentStopsToBeServed, remoteStopIndex);
		Geometry bufferWithoutEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), true);
		Geometry bufferWithEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), false);
		Geometry buffer = bufferWithEndCaps.difference(bufferWithoutEndCaps);
		
		Set<Id<TransitStopFacility>> stopsUsed = this.getStopsUsed(oldPlan.getLine().getRoutes().values());
		TransitStopFacility newStop = this.drawRandomStop(buffer, operator.getRouteProvider(), stopsUsed);
		
		if (newStop == null) {
			return null;
		}
		
		ArrayList<TransitStopFacility> newStopsToBeServed = this.addStopToExistingStops(baseStop, currentStopsToBeServed.get(remoteStopIndex), currentStopsToBeServed, newStop);
		
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
		ArrayList<TransitStopFacility> newStopsToBeServed = new ArrayList<>(currentStopsToBeServed);
		
		// decide which stop is closer
		if (CoordUtils.calcEuclideanDistance(baseStop.getCoord(), newStop.getCoord()) < CoordUtils.calcEuclideanDistance(remoteStop.getCoord(), newStop.getCoord())) {
			// baseStop is closer - insert before baseStop
			newStopsToBeServed.add(0, newStop);
		} else {
			// remote stop is closer or both have the same distance - add after remote stop
			newStopsToBeServed.add(newStopsToBeServed.indexOf(remoteStop) + 1, newStop);
		}
		
		return newStopsToBeServed;
	}

	/**
	 * Returns stop index instead of the stop, in order to cater for stops which are served multiple times
	 * 
	 * @param stops
	 * @return index of the stop which is half way on the route from start stop over all stops back to the start stop
	 */
	private int findStopIndexWithLargestDistance(ArrayList<TransitStopFacility> stops) {
		double totatDistance = 0;
		Map<Integer, Double> distFromStart2StopIndex = new HashMap<>();
		TransitStopFacility previousStop = stops.get(0);
		
		for (int i = 0; i < stops.size(); i++) {
			TransitStopFacility currentStop = stops.get(i);
			totatDistance = totatDistance + CoordUtils.calcEuclideanDistance(previousStop.getCoord(), currentStop.getCoord());
			distFromStart2StopIndex.put(i, totatDistance);
			previousStop = currentStop;
		}
		// add leg from last to first stop
		totatDistance = totatDistance + CoordUtils.calcEuclideanDistance(previousStop.getCoord(), stops.get(0).getCoord());
		
		// first terminus is first stop in stops, other terminus is stop half way on the
		// circular route beginning at the first stop
		for (int i = 1; i < stops.size(); i++) {
			if (distFromStart2StopIndex.get(i) >= totatDistance / 2) {
				if ( Math.abs(totatDistance / 2 - distFromStart2StopIndex.get(i - 1)) > 
					 Math.abs(totatDistance / 2 - distFromStart2StopIndex.get(i)) ) {
					return i;
				} else {
					return i - 1;
				}
			}
		}

		return 0;
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

	private List<Geometry> createGeometryFromStops(ArrayList<TransitStopFacility> stops, int remoteStopIndex) {
		List<Geometry> geometries = new LinkedList<>();
		
		ArrayList<Coordinate> coords = new ArrayList<>();
		for (int i = 0; i < stops.size(); i++) {
			TransitStopFacility stop = stops.get(i);
			if (i == remoteStopIndex) {
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

	public String getStrategyName() {
		return EndRouteExtension.STRATEGY_NAME;
	}
}
