/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandlingWithCycle.java
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

package playground.yu.analysis;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
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

public class EventsHandlingWithCycle implements BeforeMobsimListener,
		AfterMobsimListener, IterationEndsListener, ShutdownListener {
	private int cycle = 1;
	final static private Logger log = Logger
			.getLogger(EventsHandlingWithCycle.class);

	private final EventsManagerImpl eventsManager;
	private final List<EventWriter> eventWriters = new LinkedList<EventWriter>();

	public EventsHandlingWithCycle(EventsManagerImpl eventsManager) {
		this.eventsManager = eventsManager;
	}

	public EventsHandlingWithCycle(EventsManagerImpl eventsManager, int cycle) {
		this.eventsManager = eventsManager;
		this.cycle = cycle;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		eventsManager.resetHandlers(event.getIteration());
		eventsManager.resetCounter();

		if (controler.getWriteEventsInterval() > 0
				&& event.getIteration() % controler.getWriteEventsInterval() == 0) {
			for (EventsFileFormat format : controler.getConfig().controler()
					.getEventsFileFormats()) {
				switch (format) {
				case txt:
					eventWriters.add(new EventWriterTXT(event.getControler()
							.getControlerIO().getIterationFilename(
									event.getIteration(),
									Controler.FILENAME_EVENTS_TXT)));
					break;
				case xml:
					eventWriters.add(new EventWriterXML(event.getControler()
							.getControlerIO().getIterationFilename(
									event.getIteration(),
									Controler.FILENAME_EVENTS_XML)));
					break;
				default:
					log.warn("Unknown events file format specified: "
							+ format.toString() + ".");
				}
			}
			for (EventWriter writer : eventWriters) {
				controler.getEvents().addHandler(writer);
			}
		}
		/* differs from EventsHandling */
		if (event.getIteration() % cycle == 0) {
			controler.getVolumes().reset(event.getIteration());
			eventsManager.addHandler(controler.getVolumes());
		}

		// init for event processing of new iteration
		eventsManager.initProcessing();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		Controler controler = event.getControler();
		int iteration = event.getIteration();
		/*
		 * cdobler, nov'10 Moved this code here from
		 * Controler.CoreControlerListener.notifyAfterMobsim(...). It ensures,
		 * that if a ParallelEventsManager is used, all events are processed
		 * before the AfterMobSimListeners are informed. Otherwise e.g. usage of
		 * ParallelEventsManager and RoadPricing was not possible - MATSim
		 * crashed. After this command, the ParallelEventsManager behaves like
		 * the non-parallel implementation, therefore the main thread will have
		 * to wait until a created event has been handled.
		 */
		eventsManager.finishProcessing();

		/*
		 * moved to notifyIterationEnds(...) cdobler, Nov'10
		 */
		// for (EventWriter writer : this.eventWriters) {
		// writer.closeFile();
		// event.getControler().getEvents().removeHandler(writer);
		// }
		// this.eventWriters.clear();

		if (iteration % cycle == 0
				&& iteration > event.getControler().getFirstIteration()
		// || iteration % 10 >= 6
		) {
			controler.getLinkStats().addData(controler.getVolumes(),
					controler.getTravelTimeCalculator());
		}

		if (iteration % cycle == 0 && iteration > controler.getFirstIteration()) {
			eventsManager.removeHandler(controler.getVolumes());
			controler.getLinkStats().writeFile(
					controler.getControlerIO().getOutputFilename(
							"it" + iteration + "."
									+ Controler.FILENAME_LINKSTATS));
		}

		if (controler.getLegTimes() != null) {
			controler.getLegTimes().writeStats(
					controler.getControlerIO().getIterationFilename(iteration,
							"tripdurations.txt"));
			// - print averages in log
			log.info("["
					+ iteration
					+ "] average trip duration is: "
					+ (int) controler.getLegTimes().getAverageTripDuration()
					+ " seconds = "
					+ Time.writeTime(controler.getLegTimes()
							.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		/*
		 * Events that are produced after the Mobsim has ended, e.g. by the
		 * RoadProcing module, should also be written to the events file.
		 */
		for (EventWriter writer : eventWriters) {
			writer.closeFile();
			event.getControler().getEvents().removeHandler(writer);
		}
		eventWriters.clear();
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (EventWriter writer : eventWriters) {
			writer.closeFile();
		}
	}

}