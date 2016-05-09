/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author mrieser
 *
 * Counts the number of persons departed, arrived or got stuck per time bin
 * based on events.
 *
 * The chart plotting was moved to its own class.
 * This class could be moved to trafficmonitoring.
 *
 */
public class LegHistogram implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private Population population;
	private int iteration = 0;
	private final int binSize;
	private final int nofBins;
	private final Map<String, DataFrame> data = new TreeMap<>();

	@Inject
	LegHistogram(Population population, EventsManager eventsManager) {
		this(300);
		this.population = population;
		eventsManager.addHandler(this);
	}

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public LegHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.nofBins = nofBins;
		reset(0);
	}

	/** Creates a new LegHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public LegHistogram(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}

	/* Implementation of EventHandler-Interfaces */

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		int index = getBinIndex(event.getTime());
		if ((population == null || population.getPersons().keySet().contains(event.getPersonId())) && event.getLegMode() != null) {
			DataFrame dataFrame = getDataForMode(event.getLegMode());
			dataFrame.countsDep[index]++;
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		int index = getBinIndex(event.getTime());
		if ((population == null || population.getPersons().keySet().contains(event.getPersonId())) && event.getLegMode() != null) {
			DataFrame dataFrame = getDataForMode(event.getLegMode());
			dataFrame.countsArr[index]++;
		}
	}

	@Override
	public void handleEvent(final PersonStuckEvent event) {
		int index = getBinIndex(event.getTime());
		if ((population == null || population.getPersons().keySet().contains(event.getPersonId())) && event.getLegMode() != null) {
			DataFrame dataFrame = getDataForMode(event.getLegMode());
			dataFrame.countsStuck[index]++;
		}
	}

	@Override
	public void reset(final int iter) {
		this.iteration = iter;
		this.data.clear();
	}

	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		try (PrintStream stream = new PrintStream(new File(filename))) {
			write(stream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		stream.print("time\ttime\tdepartures_all\tarrivals_all\tstuck_all\ten-route_all");
		for (String legMode : this.data.keySet()) {
			stream.print("\tdepartures_" + legMode + "\tarrivals_" + legMode + "\tstuck_" + legMode + "\ten-route_" + legMode);
		}
		stream.print("\n");
		int allEnRoute = 0;
		int[] modeEnRoute = new int[this.data.size()];
        DataFrame allModesData = getAllModesData();
        for (int i = 0; i < allModesData.countsDep.length; i++) {
			// data about all modes
			allEnRoute = allEnRoute + allModesData.countsDep[i] - allModesData.countsArr[i] - allModesData.countsStuck[i];
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			stream.print("\t" + allModesData.countsDep[i] + "\t" + allModesData.countsArr[i] + "\t" + allModesData.countsStuck[i] + "\t" + allEnRoute);

			// data about single modes
			int mode = 0;
			for (DataFrame dataFrame : this.data.values()) {
				modeEnRoute[mode] = modeEnRoute[mode] + dataFrame.countsDep[i] - dataFrame.countsArr[i] - dataFrame.countsStuck[i];
				stream.print("\t" + dataFrame.countsDep[i] + "\t" + dataFrame.countsArr[i] + "\t" + dataFrame.countsStuck[i] + "\t" + modeEnRoute[mode]);
				mode++;
			}

			// new line
			stream.print("\n");
		}
	}

    /**
	 * @return number of departures per time-bin, for all legs
	 */
	public int[] getDepartures() {
		return this.getAllModesData().countsDep;
	}

	/**
	 * @return number of all arrivals per time-bin, for all legs
	 */
	public int[] getArrivals() {
		return this.getAllModesData().countsArr;
	}

	/**
	 * @return number of all vehicles that got stuck in a time-bin, for all legs
	 */
	public int[] getStuck() {
		return this.getAllModesData().countsStuck;
	}

	/**
	 * @return Set of all transportation modes data is available for
	 */
	public Set<String> getLegModes() {
		return this.data.keySet();
	}

	/**
	 * @param legMode transport mode
	 * @return number of departures per time-bin, for all legs with the specified mode
	 */
	public int[] getDepartures(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			return new int[0];
		}
		return dataFrame.countsDep.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number of all arrivals per time-bin, for all legs with the specified mode
	 */
	public int[] getArrivals(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			return new int[0];
		}
		return dataFrame.countsArr.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number of vehicles that got stuck in a time-bin, for all legs with the specified mode
	 */
	public int[] getStuck(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			return new int[0];
		}
		return dataFrame.countsStuck.clone();
	}

    int getIteration() {
        return iteration;
    }

    DataFrame getAllModesData() {
        DataFrame result = new DataFrame(this.binSize, this.nofBins + 1);
        for (DataFrame byMode : data.values()) {
            for (int i=0;i<result.countsDep.length;++i) {
                result.countsDep[i] += byMode.countsDep[i];
            }
            for (int i=0;i<result.countsArr.length;++i) {
                result.countsArr[i] += byMode.countsArr[i];
            }
            for (int i=0;i<result.countsStuck.length;++i) {
                result.countsStuck[i] += byMode.countsStuck[i];
            }
        }
        return result;
    }

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	DataFrame getDataForMode(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			dataFrame = new DataFrame(this.binSize, this.nofBins + 1); // +1 for all times out of our range
			this.data.put(legMode, dataFrame);
		}
		return dataFrame;
	}

	static class DataFrame {
		final int[] countsDep;
		final int[] countsArr;
		final int[] countsStuck;
        final int binSize;

        public DataFrame(final int binSize, final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
			this.countsStuck = new int[nofBins];
            this.binSize = binSize;
		}
	}

}
