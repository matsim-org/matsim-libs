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
package playground.benjamin.internalization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.EmissionModule;

/**
 * @author benjamin
 *
 */
public class InternalizeEmissionsControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(InternalizeEmissionsControlerListener.class);
	
	Vehicles emissionVehicles;
	Controler controler;
	EmissionModule emissionModule;
	String emissionEventOutputFile;
	EventWriterXML emissionEventWriter;


	public InternalizeEmissionsControlerListener(Vehicles emissionVehicles) {
		this.emissionVehicles = emissionVehicles;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		
		Scenario scenario = controler.getScenario() ;
		emissionModule = new EmissionModule(scenario, this.emissionVehicles);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EventsManager eventsManager = controler.getEvents();
		eventsManager.addHandler(emissionModule.getWarmEmissionsHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionsHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();
		emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
		
		logger.info("creating new emission events writer...");
		emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		logger.info("adding emission events writer to emission events stream...");
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info("removing emisssion events writer from emission events stream...");
		emissionModule.getEmissionEventsManager().removeHandler(emissionEventWriter);
		logger.info("closing emission events file...");
		emissionEventWriter.closeFile();
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}
	
}