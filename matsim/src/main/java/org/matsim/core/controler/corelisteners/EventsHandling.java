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
import org.matsim.analysis.CalcLegTimes;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.misc.Time;

public class EventsHandling implements BeforeMobsimListener, 
	AfterMobsimListener, IterationEndsListener, ShutdownListener {

	final static private Logger log = Logger.getLogger(EventsHandling.class);
	
	private final EventsManagerImpl eventsManager;
	private List<EventWriter> eventWriters = new LinkedList<EventWriter>();

	private int writeEventsInterval;
    Set<EventsFileFormat> eventsFileFormats ;
	
	ControlerIO controlerIO ;
	
	CalcLegTimes legTimes ;
	
	boolean calledViaOldConstructor = false ;
	
	/**
	 * @param eventsManager
	 * @param writeEventsInterval
	 * @param eventsFileFormats
	 * @param controlerIO
	 * @param legTimes -- yyyy does not belong here; is here for historic reasons since legTimes, as an events handler,
	 * does not know when an iteration is over. kai, jun'12
	 */
	public EventsHandling(EventsManagerImpl eventsManager, int writeEventsInterval, Set<EventsFileFormat> eventsFileFormats,
			ControlerIO controlerIO, CalcLegTimes legTimes ) {
		this.eventsManager = eventsManager ;
		this.writeEventsInterval = writeEventsInterval ;
		this.eventsFileFormats = eventsFileFormats ;
		this.controlerIO = controlerIO ;
		this.legTimes = legTimes ;
	}
	
	@Deprecated // use other constructor instead; do not assume that material can be retrieved from the Controler object.  
	// kai, jun'12
	public EventsHandling( EventsManagerImpl eventsManager ) {
		this.eventsManager = eventsManager ;
		this.calledViaOldConstructor = true ;
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		if ( calledViaOldConstructor ) {
			this.writeEventsInterval = event.getControler().getWriteEventsInterval() ;
			this.eventsFileFormats = event.getControler().getConfig().controler().getEventsFileFormats() ;
			this.controlerIO = event.getControler().getControlerIO() ;
			this.legTimes = event.getControler().getLegTimes() ;
		}
		
		eventsManager.resetHandlers(event.getIteration());
		eventsManager.resetCounter();

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
		
		int iteration = event.getIteration();
		/*
		 * cdobler, nov'10
		 * Moved this code here from Controler.CoreControlerListener.notifyAfterMobsim(...).
		 * It ensures, that if a ParallelEventsManager is used, all events are processed before
		 * the AfterMobSimListeners are informed. Otherwise e.g. usage of ParallelEventsManager and
		 * RoadPricing was not possible - MATSim crashed.
		 * After this command, the ParallelEventsManager behaves like the non-parallel
		 * implementation, therefore the main thread will have to wait until a created event has
		 * been handled.
		 */
		eventsManager.finishProcessing();

		if (legTimes != null) {
			legTimes.writeStats(controlerIO.getIterationFilename(iteration, "tripdurations.txt"));
			// - print averages in log
			log.info("[" + iteration + "] average trip (probably: leg) duration is: " + (int) legTimes.getAverageTripDuration()
					+ " seconds = " + Time.writeTime(legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
			// trips are from "true" activity to "true" activity.  legs may also go
			// from/to ptInteraction activity.  This, in my opinion "legs" is the correct (matsim) term
			// kai, jul'11

		}
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