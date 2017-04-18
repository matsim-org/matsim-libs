/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.example;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;


/**
 *
 * @author benjamin
 *
 */
public class EmissionControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener {
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	private String emissionEventOutputFile;
	private Integer lastIteration;

	@Inject private MatsimServices controler;
	@Inject private EmissionModule emissionModule;

	private EventWriterXML emissionEventWriter;

	// I dont see, a purpose of this now. Amit Mar'17

	public EmissionControlerListener() {
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		lastIteration = controler.getConfig().controler().getLastIteration();
		logger.info("emissions will be calculated for iteration " + lastIteration);
		
		EventsManager eventsManager = controler.getEvents();
		// commenting the following lines could cause a problem if emission events are skipped.
        // In that case, just use the events manager which is passed to the emission module. Amit Apr'17
//		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
//		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();

		if(lastIteration.equals(iteration)){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if(lastIteration.equals(event.getIteration())){
			logger.info("closing emission events file...");
			emissionEventWriter.closeFile();
			emissionModule.writeEmissionInformation();
		}
	}
}