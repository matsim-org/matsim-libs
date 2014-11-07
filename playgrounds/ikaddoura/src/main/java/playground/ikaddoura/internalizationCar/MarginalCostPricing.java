/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */

package playground.ikaddoura.internalizationCar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.analysis.extCost.ExtCostEventHandler;
import playground.ikaddoura.analysis.extCost.TripInfoWriter;

/**
 * @author ikaddoura
 *
 */

public class MarginalCostPricing implements StartupListener, IterationEndsListener {
	private static final Logger log = Logger.getLogger(MarginalCostPricing.class);

	private final ScenarioImpl scenario;
	private TollHandler tollHandler;
	private MarginalCongestionHandlerImplV3 congestionHandler;
	private MarginalCostPricingCarHandler pricingHandler;
	private ExtCostEventHandler extCostHandler;
	
	public MarginalCostPricing(ScenarioImpl scenario, TollHandler tollHandler){
		this.scenario = scenario;
		this.tollHandler = tollHandler;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();
		
		congestionHandler = new MarginalCongestionHandlerImplV3(eventsManager, scenario);
		pricingHandler = new MarginalCostPricingCarHandler(eventsManager, scenario);
		extCostHandler = new ExtCostEventHandler(this.scenario, true);
		
		eventsManager.addHandler(congestionHandler);
		eventsManager.addHandler(pricingHandler);
		eventsManager.addHandler(tollHandler);
		eventsManager.addHandler(extCostHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Set average tolls for each link Id and time bin...");
		tollHandler.setLinkId2timeBin2avgToll();
		log.info("Set average tolls for each link Id and time bin... Done.");
		
		// write out analysis every iteration
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");
		congestionHandler.writeCongestionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");
		TripInfoWriter writerCar = new TripInfoWriter(extCostHandler, event.getControler().getControlerIO().getIterationPath(event.getIteration()));
		writerCar.writeDetailedResults(TransportMode.car);
		writerCar.writeAvgTollPerDistance(TransportMode.car);
		writerCar.writeAvgTollPerTimeBin(TransportMode.car);
	}

}
