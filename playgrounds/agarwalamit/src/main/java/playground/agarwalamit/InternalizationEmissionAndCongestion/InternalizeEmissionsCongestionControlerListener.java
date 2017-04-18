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

import com.google.inject.Inject;
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
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.vsp.airPollution.flatEmissions.EmissionInternalizationHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author amit after Benjamin and Ihab
 */
public class InternalizeEmissionsCongestionControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private final Logger logger = Logger.getLogger(InternalizeEmissionsCongestionControlerListener.class);

	private MatsimServices controler;
	@Inject private EmissionModule emissionModule;
	@Inject private EmissionCostModule emissionCostModule;
	private String emissionEventOutputFile;
	private EventWriterXML emissionEventWriter;
	private EmissionInternalizationHandler emissionInternalizationHandler;
	private Set<Id<Link>> hotspotLinks;

	int iteration;
	int firstIt;
	int lastIt;

	@Inject private MutableScenario scenario;
	private final TollHandler tollHandler;
	private CongestionHandlerImplV3 congestionHandler;

	public InternalizeEmissionsCongestionControlerListener(TollHandler tollHandler) {
		this.tollHandler = tollHandler;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.controler = event.getServices();

		EventsManager eventsManager = this.controler.getEvents();
		this.congestionHandler = new CongestionHandlerImplV3(eventsManager, this.scenario);

		eventsManager.addHandler(this.congestionHandler);
		eventsManager.addHandler(new MarginalCongestionPricingHandler(eventsManager, scenario));
		eventsManager.addHandler(this.tollHandler);

		this.firstIt = this.controler.getConfig().controler().getFirstIteration();
		this.lastIt = this.controler.getConfig().controler().getLastIteration();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();

		this.logger.info("creating new emission internalization handler...");
		this.emissionInternalizationHandler = new EmissionInternalizationHandler(this.controler, this.emissionCostModule, this.hotspotLinks);
		this.logger.info("adding emission internalization module to emission events stream...");
		this.emissionModule.getEmissionEventsManager().addHandler(this.emissionInternalizationHandler);

		if(this.iteration == this.firstIt || this.iteration == this.lastIt){
			this.emissionEventOutputFile = this.controler.getControlerIO().getIterationFilename(this.iteration, "emission.events.xml.gz");
			this.logger.info("creating new emission events writer...");
			this.emissionEventWriter = new EventWriterXML(this.emissionEventOutputFile);
			this.logger.info("adding emission events writer to emission events stream...");
			this.emissionModule.getEmissionEventsManager().addHandler(this.emissionEventWriter);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		this.logger.info("removing emission internalization module from emission events stream...");
		this.logger.info("Set average tolls for each link Id and time bin.");
		this.tollHandler.setLinkId2timeBin2avgToll();
		this.emissionModule.getEmissionEventsManager().removeHandler(this.emissionInternalizationHandler);

		if(this.iteration == this.firstIt || this.iteration == this.lastIt){
			this.logger.info("removing emission events writer from emission events stream...");
			this.emissionModule.getEmissionEventsManager().removeHandler(this.emissionEventWriter);
			this.logger.info("closing emission events file...");
			this.emissionEventWriter.closeFile();
		}

		// write out toll statistics every iteration
		this.tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");

		// write out congestion statistics every iteration
		this.congestionHandler.writeCongestionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.emissionModule.writeEmissionInformation();
	}

	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}
}