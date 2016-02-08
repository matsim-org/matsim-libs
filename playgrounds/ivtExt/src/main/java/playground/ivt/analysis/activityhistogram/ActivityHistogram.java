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

package playground.ivt.analysis.activityhistogram;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.PersonExperiencedActivity;
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
@Singleton
public class ActivityHistogram implements ActivityHandler, ActivityStartEventHandler {

	private Population population;
	private int iteration = 0;
	private final int binSize;
	private final int nofBins;
	private final Map<String, DataFrame> data = new TreeMap<>();

	@Inject
	ActivityHistogram(Population population, EventsManager eventsManager) {
		this(300);
		this.population = population;
	}


	@Override
	public void handleActivity(PersonExperiencedActivity personExperiencedActivity) {
		// cannot handle all starts from this method, because it would require calling "finish"
		// on the EventsToActivity instance, which is complicated...
		if (personExperiencedActivity.getActivity().getStartTime() == Time.UNDEFINED_TIME  ) {
			handleStart( 0,
					personExperiencedActivity.getActivity().getType(),
					personExperiencedActivity.getAgentId());
		}
		if (personExperiencedActivity.getActivity().getEndTime() != Time.UNDEFINED_TIME ) {
			handleEnd(
					personExperiencedActivity.getActivity().getEndTime(),
					personExperiencedActivity.getActivity().getType(),
					personExperiencedActivity.getAgentId());
		}
	}

	/**
	 * Creates a new ActivityHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public ActivityHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.nofBins = nofBins;
		reset(0);
	}

	/** Creates a new ActivityHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public ActivityHistogram(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}


	public void handleStart(
			final double time,
			final String actType,
			final Id<Person> personId) {
		int index = getBinIndex(time);
		if ((population == null || population.getPersons().keySet().contains( personId ) && actType != null)) {
			DataFrame dataFrame = getDataForType(actType);
			dataFrame.countsStart[index]++;
		}
	}


	@Override
	public void handleEvent(ActivityStartEvent event) {
		handleStart(event.getTime(), event.getActType(), event.getPersonId());
	}

	public void handleEnd(
			final double time,
			final String actType,
			final Id<Person> personId) {
		int index = getBinIndex(time);
		if ((population == null || population.getPersons().keySet().contains( personId ) && actType != null)) {
			DataFrame dataFrame = getDataForType(actType);
			dataFrame.countsEnd[index]++;
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
		stream.print("time\ttime_s");
		for (String type : this.data.keySet()) {
			stream.print("\tstarts_" + type + "\tends_" + type + "\tin_act_" + type);
		}
		stream.print("\n");
		int[] inActPerType = new int[this.data.size()];
        for (int i = 0; i < nofBins; i++) {
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			// data about single modes
			int mode = 0;
			for (DataFrame dataFrame : this.data.values()) {
				inActPerType[mode] = inActPerType[mode] + dataFrame.countsStart[i] - dataFrame.countsEnd[i];
				stream.print("\t" + dataFrame.countsStart[i] + "\t" + dataFrame.countsEnd[i] + "\t" + inActPerType[mode]);
				mode++;
			}

			// new line
			stream.print("\n");
		}
	}

    int getIteration() {
        return iteration;
    }

	public Set<String> getTypes() {
		return data.keySet();
	}

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	DataFrame getDataForType(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			dataFrame = new DataFrame(this.binSize, this.nofBins + 1); // +1 for all times out of our range
			this.data.put(legMode, dataFrame);
		}
		return dataFrame;
	}

	static class DataFrame {
		final int[] countsStart;
		final int[] countsEnd;
        final int binSize;

        public DataFrame(final int binSize, final int nofBins) {
			this.countsStart = new int[nofBins];
			this.countsEnd = new int[nofBins];
            this.binSize = binSize;
		}
	}

}
