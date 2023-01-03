/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.utils.collections.Tuple;

import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

/**
 * Counts departures and arrivals of not strictly defined entities that move through spacetime (persons,
 * vehicles, ...) based on a spatial and temporal grid. 
 * 
 * @author tkohl / Senozon
 *
 */
class MovingEntityCounter implements PenaltyGenerator {

	private final TLongIntMap[] carArrivals;
	private final TLongIntMap initialLoad;
	private final HectareMapper hectareMapper;
	private final int timeBinSize;
	private final int numberOfTimeBins;
	
	/**
	 * Initializes the space-time-bins and sets up the initial load.
	 * 
	 * @param initialPositions A Collection containing for each specimen a Tuple with its position at
	 * the start of the simulation and its weight
	 * @param timeBinSize The size of one timebin in seconds. Reasonable values are between one minute
	 * and one hour. Linear memory impact, constant performance impact
	 * @param endTime The endtime of the last "real" timebin in seconds. After this there is an overflow
	 * timebin without end time. Linear memory impact, constant performance impact
	 * @param spatialGridSize The size of one gridcell in the spatial grid in CRS units. Reasonable values
	 * are between 100 and 1000. Pretty surely less-than-quadratic memory and performance impact.
	 */
	public MovingEntityCounter(Collection<Tuple<Coord, Integer>> initialPositions, int timeBinSize, int endTime, int spatialGridSize) {
		this.timeBinSize = timeBinSize;
		this.hectareMapper = new HectareMapper(spatialGridSize);
		this.numberOfTimeBins = TimeBinUtils.getTimeBinCount(endTime, timeBinSize);
		this.carArrivals = new TLongIntMap[numberOfTimeBins];
		
		this.initialLoad = new TLongIntHashMap();
		for (Tuple<Coord, Integer> carTuple : initialPositions) {
			this.initialLoad.adjustOrPutValue(this.hectareMapper.getKey(carTuple.getFirst()), carTuple.getSecond(), carTuple.getSecond());
		}
		
		reset();
	}
	
	/**
	 * @see #handleArrival(int, double, double, int)
	 */
	public int handleArrival(int time, Coord coord, int weight) {
		return handleArrival(time, coord.getX(), coord.getY(), weight);
	}
	
	/**
	 * Handles the arrival of an entity at a given time at a given spatial coordinate. The weight parameter
	 * allows to account for samples that should represent the whole ensemble, i.e. one entity with weight 10
	 * has the same effect as 10 entities with weight 1.
	 * 
	 * @param time The time (in seconds) since the start of the simulation
	 * @param x The x coordinate of the arrival point in the simulation CRS
	 * @param y The y coordinate of the arrival point in the simulation CRS
	 * @param weight Number of how many "real world" entities this specimen represents
	 * 
	 * @return The number of "real world" arrivals in this space-time-bin after handling the current arrival.
	 * Negative arrivals are equivalent to departures. Therefore a return value of 0 not necessarily means no
	 * arrivals, but only that there are as many arrivals as departures (which still could be 0)
	 */
	public int handleArrival(int time, double x, double y, int weight) {
		return this.carArrivals[getTimeBin(time)].adjustOrPutValue(this.hectareMapper.getKey(x, y), weight, weight);
	}
	
	/**
	 * @see #handleDeparture(int, double, double, int)
	 */
	public int handleDeparture(int time, Coord coord, int weight) {
		return handleDeparture(time, coord.getX(), coord.getY(), weight);
	}
	
	/**
	 * Handles the departure of an entity at a given time at a given spatial coordinate. The weight parameter
	 * allows to account for samples that should represent the whole ensemble, i.e. one entity with weight 10
	 * has the same effect as 10 entities with weight 1.
	 * 
	 * @param time The time (in seconds) since the start of the simulation
	 * @param x The x coordinate of the arrival point in the simulation CRS
	 * @param y The y coordinate of the arrival point in the simulation CRS
	 * @param weight Number of how many "real world" entities this specimen represents
	 * 
	 * @return The number of "real world" arrivals in this space-time-bin after handling the current arrival.
	 * Negative arrivals are equivalent to departures. Therefore a return value of 0 not necessarily means no
	 * no departures, but only that there are as many arrivals as departures (which still could be 0)
	 */
	public int handleDeparture(int time, double x, double y, int weight) {
		return this.carArrivals[getTimeBin(time)].adjustOrPutValue(this.hectareMapper.getKey(x, y), -weight, -weight);
	}
	
	/**
	 * Calculates for each space-time-bin the number of specimen based on the initial load received in the
	 * constructor and the departures and arrivals collected with {@linkplain #handleArrival(int, double, double, int)}
	 * and {@linkplain #handleDeparture(int, double, double, int)} and then generates a {@linkplain PenaltyCalculator}
	 * based on the result.
	 */
	@Override
	public PenaltyCalculator generatePenaltyCalculator() {
		TLongIntMap[] cars = new TLongIntMap[this.carArrivals.length];
		cars[0] = new TLongIntHashMap(this.initialLoad);
		for (int i = 1; i < this.carArrivals.length; i++) {
			cars[i] = new TLongIntHashMap(cars[i-1]);
			TLongIntIterator iter = this.carArrivals[i-1].iterator();
			while (iter.hasNext()) {
				iter.advance();
				cars[i].adjustOrPutValue(iter.key(), iter.value(), iter.value());
			}
		}
		
		return new PenaltyCalculator(cars, this.timeBinSize, this.hectareMapper);
	}
	
	/**
	 * sets all arrival bins to 0
	 */
	@Override
	public void reset() {
		for (int i = 0; i < this.carArrivals.length; i++) {
			this.carArrivals[i] = new TLongIntHashMap();
		}
	}
	
	private int getTimeBin(int time) {
		return TimeBinUtils.getTimeBinIndex(time, this.timeBinSize, this.numberOfTimeBins);
	}
}
