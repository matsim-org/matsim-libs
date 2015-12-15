/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.counts;

import com.google.inject.Inject;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * Counts the daily passengers on a given link.
 *
 * This is a customized version of org.matsim.pt.counts.OccupancyAnalyzer by yChen and mrieser
 * @author boescpa
 */
public class PTCountsIVTBaseline implements StartupListener, IterationEndsListener, IterationStartsListener {

	private final PTCountsEventHandler eventHandler;
	private final EventsManager events;
	private final OutputDirectoryHierarchy controlerIO;
	private final Config config;

	private boolean recordCounts;

	@Inject
	private PTCountsIVTBaseline(PTCountsEventHandler ptCountsEventHandler, EventsManager events,
							   OutputDirectoryHierarchy controlerIO, Config config) {
		this.eventHandler = ptCountsEventHandler;
		this.events = events;
		this.controlerIO = controlerIO;
		this.config = config;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.recordCounts = true;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int countsInterval = this.config.ptCounts().getPtCountsInterval();
		//int countsInterval = event.getControler().getConfig().ptCounts().getPtCountsInterval();
		if (event.getIteration() % countsInterval == 0) {
			this.recordCounts = true;
			this.events.addHandler(this.eventHandler);
			this.eventHandler.reset(event.getIteration());
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.recordCounts) {
			this.eventHandler.write(this.controlerIO.getIterationFilename(event.getIteration(), "ptCounts.txt"));
			this.events.removeHandler(this.eventHandler);
		}
		this.recordCounts = false;
	}
}
