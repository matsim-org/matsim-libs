/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramInstance.java
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

package playground.christoph.events.implementations;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

import playground.christoph.events.EventHandlerInstance;

/**
 * @author mrieser
 * @author cdobler
 *
 * Counts the number of vehicles departed, arrived or got stuck per time bin
 * based on events.
 */
public class LegHistogramInstance implements EventHandlerInstance, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private final int binSize;
	private final int nofBins;
	private final Map<String, ModeData> data = new TreeMap<String, ModeData>();
	private ModeData allModesData = null;

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public LegHistogramInstance(final int binSize, final int nofBins) {
		this.binSize = binSize;
		this.nofBins = nofBins;
		reset(0);
	}

	/** Creates a new LegHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public LegHistogramInstance(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}

	/* Implementation of EventHandler-Interfaces */

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsDep[index]++;
		if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsDep[index]++;
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsArr[index]++;
		if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsArr[index]++;
		}
	}

	@Override
	public void handleEvent(final PersonStuckEvent event) {
		int index = getBinIndex(event.getTime());
		this.allModesData.countsStuck[index]++;
		if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsStuck[index]++;
		}
	}

	@Override
	public void reset(final int iter) {
		this.allModesData = new ModeData(this.nofBins + 1);
		this.data.clear();
	}

	/**
	 * @return number of departures per time-bin, for all legs
	 */
	/*package*/ int[] getDepartures() {
		return this.allModesData.countsDep.clone();
	}

	/**
	 * @return number of all arrivals per time-bin, for all legs
	 */
	/*package*/ int[] getArrivals() {
		return this.allModesData.countsArr.clone();
	}

	/**
	 * @return number of all vehicles that got stuck in a time-bin, for all legs
	 */
	/*package*/ int[] getStuck() {
		return this.allModesData.countsStuck.clone();
	}

	/**
	 * @return Set of all transportation modes data is available for
	 */
	/*package*/ Set<String> getLegModes() {
		return this.data.keySet();
	}

	/**
	 * @param legMode transport mode
	 * @return number of departures per time-bin, for all legs with the specified mode
	 */
	/*package*/ int[] getDepartures(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsDep.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number of all arrivals per time-bin, for all legs with the specified mode
	 */
	/*package*/ int[] getArrivals(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsArr.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number of vehicles that got stuck in a time-bin, for all legs with the specified mode
	 */
	/*package*/ int[] getStuck(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsStuck.clone();
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	private ModeData getDataForMode(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			modeData = new ModeData(this.nofBins + 1); // +1 for all times out of our range
			this.data.put(legMode, modeData);
		}
		return modeData;
	}

	/*package*/ static class ModeData {
		public final int[] countsDep;
		public final int[] countsArr;
		public final int[] countsStuck;

		public ModeData(final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
			this.countsStuck = new int[nofBins];
		}
	}
	
	@Override
	public void synchronize(double time) {
		// nothing to do here
	}

}
