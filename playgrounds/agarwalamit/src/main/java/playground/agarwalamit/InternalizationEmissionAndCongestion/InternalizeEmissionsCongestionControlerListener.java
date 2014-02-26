/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.InternalizationEmissionAndCongestion;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.scenario.ScenarioImpl;

import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionInternalizationHandler;
import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;
import playground.ikaddoura.internalizationCar.MarginalCostPricingCarHandler;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.vsp.emissions.EmissionModule;

/**
 * @author amit
 */
public class InternalizeEmissionsCongestionControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(InternalizeEmissionsCongestionControlerListener.class);

	Controler controler;
	EmissionModule emissionModule;
	EmissionCostModule emissionCostModule;
	String emissionEventOutputFile;
	EventWriterXML emissionEventWriter;
	EmissionInternalizationHandler emissionInternalizationHandler;
	Set<Id> hotspotLinks;

	int iteration;
	int firstIt;
	int lastIt;

	private final ScenarioImpl scenario;
	private TollHandler tollHandler;
	private MarginalCongestionHandlerImplV3 congestionHandler;


	public InternalizeEmissionsCongestionControlerListener(EmissionModule emissionModule, EmissionCostModule emissionCostModule,ScenarioImpl scenario, TollHandler tollHandler) {
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.scenario = scenario;
		this.tollHandler = tollHandler;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();

		EventsManager eventsManager = controler.getEvents();
		congestionHandler = new MarginalCongestionHandlerImplV3(eventsManager, scenario);

		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());

		eventsManager.addHandler(congestionHandler);
		eventsManager.addHandler(new MarginalCostPricingCarHandler(eventsManager, scenario));
		eventsManager.addHandler(tollHandler);

		firstIt = controler.getFirstIteration();
		lastIt = controler.getLastIteration();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		iteration = event.getIteration();

		logger.info("creating new emission internalization handler...");
		emissionInternalizationHandler = new EmissionInternalizationHandler(controler, emissionCostModule, hotspotLinks);
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

		logger.info("removing emission internalization module from emission events stream...");

		logger.info("Set average tolls for each link Id and time bin.");
		tollHandler.setLinkId2timeBin2avgToll();

		emissionModule.getEmissionEventsManager().removeHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			logger.info("removing emission events writer from emission events stream...");
			emissionModule.getEmissionEventsManager().removeHandler(emissionEventWriter);
			logger.info("closing emission events file...");
			emissionEventWriter.closeFile();
		}

		// write out toll statistics every iteration
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");

		// write out congestion statistics every iteration
		congestionHandler.writeCongestionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}

	public void setHotspotLinks(Set<Id> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}
