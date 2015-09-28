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
package org.matsim.contrib.socnetsim.usage.analysis;

import com.google.inject.Inject;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEvent;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEventHandler;
import org.matsim.core.utils.misc.Time;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author thibautd
 */
@Singleton
public class CourtesyHistogram implements CourtesyEventHandler {

	private int iteration = 0;
	private final int binSize;
	private final int nofBins;


	private final Map<String,DataFrame> dataPerActType = new TreeMap<>();

	@Inject
	CourtesyHistogram() {
		this(300);
	}

	/**
	 * Creates a new CourtesyHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public CourtesyHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.nofBins = nofBins;
		reset(0);
	}

	/** Creates a new CourtesyHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public CourtesyHistogram(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}

	/* Implementation of EventHandler-Interfaces */

	@Override
	public void handleEvent(final CourtesyEvent event) {
		int index = getBinIndex(event.getTime());
		final DataFrame data = getData( event.getActType() );
		switch ( event.getType() ) {
			case sayHelloEvent:
				data.countsHello[ index ]++;
				break;
			case sayGoodbyeEvent:
				data.countsGoodbye[ index ]++;
				break;
			default:
				throw new RuntimeException( event.getType()+"?" );
		}
	}

	private DataFrame getData(final String actType) {
		DataFrame dataFrame = this.dataPerActType.get( actType );
		if (dataFrame == null) {
			dataFrame = new DataFrame(this.binSize, this.nofBins + 1); // +1 for all times out of our range
			this.dataPerActType.put(actType, dataFrame);
		}
		return dataFrame;
	}

	@Override
	public void reset(final int iter) {
		this.iteration = iter;
		this.dataPerActType.clear();
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
		stream.print("time\ttime_s" );
		for ( String actType : dataPerActType.keySet() ) {
			stream.print("\thello_"+actType+"\tgoodbye_"+actType+"\tpairs_together_"+actType);
		}
		stream.print("\n");

		int[] pairsTogetherPerType = new int[ dataPerActType.size() ];

        for (int i = 0; i < nofBins; i++) {
			int mode = 0;
			for ( DataFrame data : dataPerActType.values() ) {
				pairsTogetherPerType[ mode ] = pairsTogetherPerType[ mode ] + data.countsHello[i] - data.countsGoodbye[i];

				stream.print(Time.writeTime(i * this.binSize) + "\t" + i * this.binSize);
				stream.print("\t" + data.countsHello[i] + "\t" + data.countsGoodbye[i] + "\t" + pairsTogetherPerType);
				mode++;
			}

			// new line
			stream.print("\n");
		}
	}


	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	int getIteration() {
		return iteration;
	}

	Map<String,DataFrame> getDataFrames() {
		return dataPerActType;
	}

	static class DataFrame {
		final int[] countsHello;
		final int[] countsGoodbye;
        final int binSize;

        public DataFrame(final int binSize, final int nofBins) {
			this.countsHello = new int[nofBins];
			this.countsGoodbye = new int[nofBins];
            this.binSize = binSize;
		}
	}

}
