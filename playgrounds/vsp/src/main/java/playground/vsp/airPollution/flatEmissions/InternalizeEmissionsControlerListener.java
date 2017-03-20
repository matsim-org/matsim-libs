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
package playground.vsp.airPollution.flatEmissions;

import java.util.Set;

import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;


/**
 * @author benjamin
 *
 */
public class InternalizeEmissionsControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(InternalizeEmissionsControlerListener.class);

	@Inject private MatsimServices controler;
	@Inject private EventsManager eventsManager;

	private EventWriterXML emissionEventWriter;
	private EmissionInternalizationHandler emissionInternalizationHandler;

	@Inject private EmissionModule emissionModule;
	@Inject private EmissionCostModule emissionCostModule;

	String emissionEventOutputFile;
	private Set<Id<Link>> hotspotLinks;
	
	private int firstIt;
	private int lastIt;

	@Override
	public void notifyStartup(StartupEvent event) {
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		firstIt = controler.getConfig().controler().getFirstIteration();
		lastIt = controler.getConfig().controler().getLastIteration();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();

		logger.info("creating new emission internalization handler...");
		emissionInternalizationHandler = new EmissionInternalizationHandler(controler, emissionCostModule, hotspotLinks );
		logger.info("adding emission internalization module to emission events stream...");
		emissionModule.getEmissionEventsManager().addHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();

		logger.info("removing emission internalization module from emission events stream...");
		emissionModule.getEmissionEventsManager().removeHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			logger.info("removing emission events writer from emission events stream...");
			emissionModule.getEmissionEventsManager().removeHandler(emissionEventWriter);
			logger.info("closing emission events file...");
			emissionEventWriter.closeFile();
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}

	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}