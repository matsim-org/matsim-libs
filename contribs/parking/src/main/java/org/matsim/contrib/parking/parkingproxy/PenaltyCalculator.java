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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.trafficmonitoring.TimeBinUtils;

import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

/**
 * <p>
 * Calculates time penalties based on the number of certain entities (cars, persons, ...) in a certain space-time-gridcell.
 * The underlying input data can not be changed after creation, however a different function to translate the number of
 * entities into a numerical penalty may be changed at any time.
 * </p>
 * <p>
 * Instances of this class typically will only be provided by {@linkplain PenaltyGenerator} implementations. This is a
 * deliberate decision to underline the immutability of this object.
 * </p>
 * 
 * @author tkohl / Senozon
 *
 */
class PenaltyCalculator {
	
	private final TLongIntMap[] numberOfEntities;
	private final int numberOfTimeBins;
	private final int timeBinSize;
	private final HectareMapper hectareMapper;
	
	private PenaltyFunction penaltyFunction;
	
	/*package*/ PenaltyCalculator(TLongIntMap[] numberOfEntities, int timeBinSize, HectareMapper hectareMapper) {
		this.numberOfEntities = numberOfEntities;
		this.numberOfTimeBins = numberOfEntities.length;
		this.timeBinSize = timeBinSize;
		this.hectareMapper = hectareMapper;
		this.setPenaltyFunction(new DefaultPenaltyFunction());
	}
	
	/**
	 * Sets the penalty function that translates the number of entities in a space-time-bin into a time-penalty
	 * 
	 * @param function
	 */
	public void setPenaltyFunction(PenaltyFunction function) {
		this.penaltyFunction = function;
	}
	
	/**
	 * Fetches the penalty for the specified space-time-bin
	 * 
	 * @param time the time for which to get the penalty
	 * @param x the x-coordinate for which to get the penalty
	 * @param y the y-coordinate for which to get the penalty
	 * @return the penalty in the space-time-bin
	 */
	public double getPenalty(double time, double x, double y) {
		time = Math.max(0,  time);
		int timebin = TimeBinUtils.getTimeBinIndex(time, this.timeBinSize, this.numberOfTimeBins);
		int cars = this.numberOfEntities[timebin].get(this.hectareMapper.getKey(x, y));
		return this.penaltyFunction.calculatePenalty(cars);
	}
	
	/**
	 * Fetches the penalty for the specified space-time-bin
	 * 
	 * @param time the time for which to get the penalty
	 * @param coord the coordinate for which to get the penalty
	 * @return the penalty in the space-time-bin
	 */
	public double getPenalty(double time, Coord coord) {
		return getPenalty(time, coord.getX(), coord.getY());
	}
	
	/**
	 * Dumps the penalties for all space-time-bins with a non-zero number of entities (there may be some with zero
	 * entities in the dump however) as a csv file with the header "{@code x;y;t;penalty}".
	 * 
	 * @param outputfile The file in which to write
	 */
	public void dump(File outputfile) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile))) {
			writer.write("x;y;t;penalty");
			writer.newLine();
			for (int tbin = 0; tbin < numberOfEntities.length; tbin++) {
				TLongIntIterator iter = this.numberOfEntities[tbin].iterator();
				while (iter.hasNext()) {
					iter.advance();
					Coord coord = hectareMapper.getCenter(iter.key());
					String entry = (int) coord.getX() + ";";
					entry += (int)coord.getY() + ";";
					entry += tbin * timeBinSize + ";";
					entry += penaltyFunction.calculatePenalty(iter.value());
					writer.write(entry);
					writer.newLine();
				}
			}
			writer.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * Creates a dummy calculator with one timebin and a spatial binsize of 100km. It returns 3600s (1h) on
	 * every request by using {@linkplain PenaltyCalculator.DummyPenaltyFunction} as function.
	 * 
	 * @return The created dummy calculator
	 */
	public static PenaltyCalculator getDummyHourCalculator() {
		TLongIntMap[] cars = new TLongIntMap[1];
		cars[0] = new TLongIntHashMap();
		PenaltyCalculator penaltyCalculator = new PenaltyCalculator(cars, 1, new HectareMapper(100000));
		penaltyCalculator.setPenaltyFunction(new DummyPenaltyFunction(3600));
		return penaltyCalculator;
	}
	
	/**
	 * Creates a dummy calculator with one timebin and a spatial binsize of 100km. It returns 0 on
	 * every request by using {@linkplain PenaltyCalculator.DummyPenaltyFunction} as function.
	 * 
	 * @return The created dummy calculator
	 */
	public static PenaltyCalculator getDummyZeroCalculator() {
		TLongIntMap[] cars = new TLongIntMap[1];
		cars[0] = new TLongIntHashMap();
		PenaltyCalculator penaltyCalculator = new PenaltyCalculator(cars, 1, new HectareMapper(100000));
		penaltyCalculator.setPenaltyFunction(new DummyPenaltyFunction(0));
		return penaltyCalculator;
	}
	
	/**
	 * The default Penalty function which assumes 2.5s of penalty per entity in the space-time-bin but never more
	 * than 900s (15min).
	 */
	public static class DefaultPenaltyFunction implements PenaltyFunction {
		@Override
		public double calculatePenalty(int numberOfCars) {
			return Math.min(numberOfCars*2.5, 900);
		}
	}
	
	/**
	 * Always returns a fixed time regardless of how many entities there are in the space-time-bin.
	 */
	public static class DummyPenaltyFunction implements PenaltyFunction {
		private final int time;
		public DummyPenaltyFunction(int time) {
			this.time = time;
		}
		@Override
		public double calculatePenalty(int numberOfCars) {
			return time;
		}
	}
}
