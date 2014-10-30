/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramListenerWithoutControler.java
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
package playground.thibautd.analysis.listeners;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * Produces histograms without requiring a Controler
 * @author thibautd
 */
public class LegHistogramListenerWithoutControler implements IterationEndsListener, IterationStartsListener {
	private final int writeGraphicInterval;
	private final LegHistogram histogram;
	private final OutputDirectoryHierarchy controlerIO;

	static private final Logger log = Logger.getLogger(LegHistogramListenerWithoutControler.class);

	public LegHistogramListenerWithoutControler(
			final int writeGraphicInterval,
			final EventsManager events,
			final OutputDirectoryHierarchy io) {
		this.writeGraphicInterval = writeGraphicInterval;
		this.histogram = new LegHistogram(300);
		events.addHandler( histogram );
		this.controlerIO = io;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.histogram.reset(event.getIteration());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.histogram.write( controlerIO.getIterationFilename(event.getIteration(), "legHistogram.txt") );
		this.printStats();

		if ( writeGraphicInterval < 1 || event.getIteration() % writeGraphicInterval == 0 ) {
			LegHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "legHistogram_all.png"));
			for (String legMode : this.histogram.getLegModes()) {
				LegHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "legHistogram_" + legMode + ".png"), legMode);
			}
		}
	}

	public void printStats() {
		int nofLegs = 0;
		for (int nofDepartures : this.histogram.getDepartures()) {
			nofLegs += nofDepartures;
		}
		log.info("number of legs:\t"  + nofLegs + "\t100%");
		for (String legMode : this.histogram.getLegModes()) {
			int nofModeLegs = 0;
			for (int nofDepartures : this.histogram.getDepartures(legMode)) {
				nofModeLegs += nofDepartures;
			}
			if (nofModeLegs != 0) {
				log.info("number of " + legMode + " legs:\t"  + nofModeLegs + "\t" + (nofModeLegs * 100.0 / nofLegs) + "%");
			}
		}
	}

}

