/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandling.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.corelisteners;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;

import com.google.inject.Inject;

public class EventsHandlingImpl implements EventsHandling, BeforeMobsimListener, 
	AfterMobsimListener, IterationEndsListener, ShutdownListener {

	final static private Logger log = Logger.getLogger(EventsHandlingImpl.class);
	
	private final EventsManager eventsManager;
	private List<EventWriter> eventWriters = new LinkedList<>();

	private int writeEventsInterval;
    
	private Set<EventsFileFormat> eventsFileFormats ;
	
	private OutputDirectoryHierarchy controlerIO ;

	@Inject
	public EventsHandlingImpl(
			final EventsManager eventsManager,
			final Config config,
			final OutputDirectoryHierarchy controlerIO ) {
		this.eventsManager = eventsManager ;
		this.writeEventsInterval = config.controler().getWriteEventsInterval();
		this.eventsFileFormats = config.controler().getEventsFileFormats();
		this.controlerIO = controlerIO ;
	}

	public EventsHandlingImpl(EventsManager eventsManager, int writeEventsInterval, Set<EventsFileFormat> eventsFileFormats,
			OutputDirectoryHierarchy controlerIO ) {
		this.eventsManager = eventsManager ;
		this.writeEventsInterval = writeEventsInterval ;
		this.eventsFileFormats = eventsFileFormats ;
		this.controlerIO = controlerIO ;
	}

    @Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        eventsManager.resetHandlers(event.getIteration());
		if ((this.writeEventsInterval > 0) && (event.getIteration() % writeEventsInterval == 0)) {
			for (EventsFileFormat format : eventsFileFormats) {
				switch (format) {
				case txt:
					this.eventWriters.add(new EventWriterTXT(controlerIO.getIterationFilename(event.getIteration(), 
							Controler.FILENAME_EVENTS_TXT)));
					break;
				case xml:
					this.eventWriters.add(new EventWriterXML(controlerIO.getIterationFilename(event.getIteration(), 
							Controler.FILENAME_EVENTS_XML)));
					break;
				default:
					log.warn("Unknown events file format specified: " + format.toString() + ".");
				}
			}
			for (EventWriter writer : this.eventWriters) {
				eventsManager.addHandler(writer);
			}
		}

		// init for event processing of new iteration
		eventsManager.initProcessing();
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		/*
		 * cdobler, nov'10
		 * Moved this code here from Controler.CoreControlerListener.notifyAfterMobsim(...).
		 * It ensures, that if a ParallelEventsManager is used, all events are processed before
		 * the AfterMobSimListeners are informed. Otherwise e.g. usage of ParallelEventsManager and
		 * RoadPricing was not possible - MATSim crashed.
		 * After this command, the ParallelEventsManager behaves like the non-parallel
		 * implementation, therefore the main thread will have to wait until a created event has
		 * been handled.
		 * 
		 * This means, this thing prevents _two_ different bad things from happening:
		 * 1.) Road pricing (for example) from starting to calculate road prices 
		 *      while Mobsim-Events are still coming in (and crashing)
		 * 2.) Later things which happen in the Controler (e.g. Scoring) from starting
		 * 	    to score while (for example) road pricing events are still coming in
		 *      (and crashing).
		 * michaz (talking to cdobler), jun'13
		 */
		eventsManager.finishProcessing();

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		/*
		 * Events that are produced after the Mobsim has ended, e.g. by the RoadProcing 
		 * module, should also be written to the events file.
		 */
		for (EventWriter writer : this.eventWriters) {
			writer.closeFile();
			this.eventsManager.removeHandler(writer);
		}
		this.eventWriters.clear();
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (EventWriter writer : this.eventWriters) {
			writer.closeFile();
		}
	}
	
}
