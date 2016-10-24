/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramListener.java
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

package playground.jbischoff.analysis;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * Integrates the {@link org.matsim.analysis.TripHistogram} into the
 * {@link org.matsim.core.controler.Controler}, so the trip histogram is
 * automatically created every iteration.
 *
 * @author mrieser,jbischoff
 */
public final class TripHistogramListener implements IterationEndsListener {

	@Inject private TripHistogram histogram;
	@Inject private ControlerConfigGroup controlerConfigGroup;
	@Inject private OutputDirectoryHierarchy controlerIO;

	static private final Logger log = Logger.getLogger(TripHistogramListener.class);

	

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.histogram.write(controlerIO.getIterationFilename(event.getIteration(), "tripHistogram.txt"));
		this.printStats();
		if (controlerConfigGroup.isCreateGraphs()) {
			TripHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "tripHistogram_all.png"));
			for (String legMode : this.histogram.getLegModes()) {
				TripHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "tripHistogram_" + legMode + ".png"), legMode);
			}
		}

	}

	private void printStats() {
		int nofLegs = 0;
		for (int nofDepartures : this.histogram.getDepartures()) {
			nofLegs += nofDepartures;
		}
		log.info("number of trips:\t"  + nofLegs + "\t100%");
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
