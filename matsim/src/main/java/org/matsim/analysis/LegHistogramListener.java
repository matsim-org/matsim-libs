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

package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import jakarta.inject.Inject;

/**
 * Integrates the {@link org.matsim.analysis.LegHistogram} into the
 * {@link org.matsim.core.controler.Controler}, so the leg histogram is
 * automatically created every iteration.
 *
 * @author mrieser
 */
final class LegHistogramListener implements IterationEndsListener, IterationStartsListener {

	@Inject private LegHistogram histogram;
	@Inject private ControllerConfigGroup controllerConfigGroup;
	@Inject private OutputDirectoryHierarchy controllerIO;

	static private final Logger log = LogManager.getLogger(LegHistogramListener.class);

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		if (ControllerUtils.isIterationActive(event, controllerConfigGroup.getLegHistogramInterval())) {
			this.histogram.reset(event.getIteration());
			event.getServices().getEvents().addHandler(this.histogram);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (ControllerUtils.isIterationActive(event, controllerConfigGroup.getLegHistogramInterval())) {
			this.histogram.write(controllerIO.getIterationFilename(event.getIteration(), "legHistogram.txt"));
			this.printStats();
			int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
			if (createGraphsInterval > 0 && event.getIteration() % createGraphsInterval == 0) {
				LegHistogramChart.writeGraphic(this.histogram, controllerIO.getIterationFilename(event.getIteration(), "legHistogram_all.png"));
				for (String legMode : this.histogram.getLegModes()) {
					LegHistogramChart.writeGraphic(this.histogram, controllerIO.getIterationFilename(event.getIteration(), "legHistogram_" + legMode + ".png"), legMode);
				}
			}
			event.getServices().getEvents().removeHandler(this.histogram);
		}
	}

	private void printStats() {
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
