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
import org.matsim.core.events.handler.EventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts the daily passengers on a given link.
 *
 * This is a customized version of org.matsim.pt.counts.OccupancyAnalyzer by yChen and mrieser
 * @author boescpa
 */
public class CountsIVTBaseline implements StartupListener, IterationEndsListener, IterationStartsListener {

	public static final String COUNTS_DELIMITER = ";";

	private final PTLinkCountsEventHandler ptLinkCountsEventHandler;
	private final PTStationCountsEventHandler ptStationCountsEventHandler;
	private final StreetLinkDailyCountsEventHandler streetLinkDailyCountsEventHandler;
	private final StreetLinkHourlyCountsEventHandler streetLinkHourlyCountsEventHandler;
	private final EventsManager events;
	private final OutputDirectoryHierarchy controlerIO;
	private final Config config;

	private boolean recordCounts;

	@Inject
	private CountsIVTBaseline(PTLinkCountsEventHandler ptLinkCountsEventHandler, PTStationCountsEventHandler ptStationCountsEventHandler,
							  StreetLinkDailyCountsEventHandler streetLinkDailyCountsEventHandler, StreetLinkHourlyCountsEventHandler streetLinkHourlyCountsEventHandler,
							  EventsManager events, OutputDirectoryHierarchy controlerIO, Config config) {
		this.ptLinkCountsEventHandler = ptLinkCountsEventHandler;
		this.ptStationCountsEventHandler = ptStationCountsEventHandler;
		this.streetLinkDailyCountsEventHandler = streetLinkDailyCountsEventHandler;
		this.streetLinkHourlyCountsEventHandler = streetLinkHourlyCountsEventHandler;
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
		if (event.getIteration() % countsInterval == 0) {
			this.recordCounts = true;
			this.events.addHandler(this.ptLinkCountsEventHandler);
			this.ptLinkCountsEventHandler.reset(event.getIteration());
			this.events.addHandler(this.ptStationCountsEventHandler);
			this.ptStationCountsEventHandler.reset(event.getIteration());
			this.events.addHandler(this.streetLinkDailyCountsEventHandler);
			this.streetLinkDailyCountsEventHandler.reset(event.getIteration());
			this.events.addHandler(this.streetLinkHourlyCountsEventHandler);
			this.streetLinkHourlyCountsEventHandler.reset(event.getIteration());
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.recordCounts) {
			this.ptLinkCountsEventHandler.write(this.controlerIO.getIterationFilename(event.getIteration(), "ptLinkCounts.csv"));
			this.events.removeHandler(this.ptLinkCountsEventHandler);
			this.ptStationCountsEventHandler.write(this.controlerIO.getIterationFilename(event.getIteration(), "ptStationCounts.csv"));
			this.events.removeHandler(this.ptStationCountsEventHandler);
			this.streetLinkDailyCountsEventHandler.write(this.controlerIO.getIterationFilename(event.getIteration(), "streetDailyCounts.csv"));
			this.events.removeHandler(this.streetLinkDailyCountsEventHandler);
			this.streetLinkHourlyCountsEventHandler.write(this.controlerIO.getIterationFilename(event.getIteration(), "streetHourlyCounts.csv"));
			this.events.removeHandler(this.streetLinkHourlyCountsEventHandler);
		}
		this.recordCounts = false;
	}
}
