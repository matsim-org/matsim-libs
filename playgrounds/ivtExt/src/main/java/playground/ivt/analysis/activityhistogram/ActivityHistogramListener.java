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

package playground.ivt.analysis.activityhistogram;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import javax.inject.Inject;

/**
 * Integrates the {@link LegHistogram} into the
 * {@link org.matsim.core.controler.Controler}, so the leg histogram is
 * automatically created every iteration.
 *
 * @author mrieser
 */
@Singleton
final class ActivityHistogramListener implements IterationEndsListener, IterationStartsListener {

	private final ActivityHistogram histogram;
	private final boolean outputGraph;

	static private final Logger log = Logger.getLogger(ActivityHistogramListener.class);
    private final OutputDirectoryHierarchy controlerIO;

    @Inject
	ActivityHistogramListener(Config config, ActivityHistogram legHistogram, OutputDirectoryHierarchy controlerIO) {
        this.controlerIO = controlerIO;
		this.histogram = legHistogram;
		this.outputGraph = config.controler().isCreateGraphs();
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.histogram.reset(event.getIteration());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.histogram.write(controlerIO.getIterationFilename(event.getIteration(), "actHistogram.txt"));
		if (this.outputGraph) {
			for (String actType : this.histogram.getTypes()) {
				ActivityHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "actHistogram_" + actType + ".png"), actType);
			}
		}

	}
}
