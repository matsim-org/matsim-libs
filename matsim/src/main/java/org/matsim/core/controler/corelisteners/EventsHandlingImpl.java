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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterJson;
import org.matsim.core.events.algorithms.EventWriterXML;

import java.io.File;
import org.matsim.core.utils.io.IOUtils;

@Singleton
final class EventsHandlingImpl implements EventsHandling, BeforeMobsimListener,
	IterationEndsListener, ShutdownListener {

	final static private Logger log = LogManager.getLogger(EventsHandlingImpl.class);

	private final EventsManager eventsManager;
	private List<EventWriter> eventWriters = new LinkedList<>();

	private int writeEventsInterval;

	private Set<EventsFileFormat> eventsFileFormats ;

	private OutputDirectoryHierarchy controlerIO ;

	private int writeMoreUntilIteration;

	@Inject
	EventsHandlingImpl(
			final EventsManager eventsManager,
			final ControllerConfigGroup config,
			final OutputDirectoryHierarchy controlerIO) {
		this.eventsManager = eventsManager;
		this.writeEventsInterval = config.getWriteEventsInterval();
		this.eventsFileFormats = config.getEventsFileFormats();
		this.controlerIO = controlerIO;
		this.writeMoreUntilIteration = config.getWriteEventsUntilIteration() ;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		eventsManager.resetHandlers(event.getIteration());
		final boolean writingEventsAtAll = this.writeEventsInterval > 0;
		final boolean regularWriteEvents = writingEventsAtAll && ( event.getIteration()>0 && event.getIteration() % writeEventsInterval == 0 ) ;
		// (w/o the "writingEventsAtAll && ..." this is a division by zero when writeEventsInterval=0. kai, apr'18)
		final boolean earlyIteration = event.getIteration() <= writeMoreUntilIteration ;
		final boolean lastIteration = event.isLastIteration();
		if (writingEventsAtAll && (regularWriteEvents||earlyIteration || lastIteration ) ) {
			for (EventsFileFormat format : eventsFileFormats) {
				switch (format) {
					case xml:
						this.eventWriters.add(new EventWriterXML(controlerIO.getIterationFilename(event.getIteration(),
								Controler.DefaultFiles.events)));
						break;
					case pb:
						// The pb dependency is optional at the moment so we search it first
						URL url = IOUtils.getFileUrl(controlerIO.getIterationFilename(event.getIteration(), Controler.DefaultFiles.eventsPb));
						try {
							Class<?> writerClass = ClassLoader.getSystemClassLoader().loadClass("org.matsim.contrib.protobuf.EventWriterPB");
							Constructor<?> constructor = writerClass.getConstructor(OutputStream.class);
							EventWriter writer = (EventWriter) constructor.newInstance(IOUtils.getOutputStream(url, false));
							this.eventWriters.add(writer);
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException("Error using the PBWriter. Please make sure protobuf contrib on the classpath, or remove pb output format.", e);
						}
						break;
					case json:
						this.eventWriters.add(new EventWriterJson(new File(controlerIO.getIterationFilename(event.getIteration(),
								Controler.DefaultFiles.eventsJson))));
						break;
					default:
						log.warn("Unknown events file format specified: " + format.toString() + ".");
				}
			}
			for (EventWriter writer : this.eventWriters) {
				eventsManager.addHandler(writer);
			}
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
