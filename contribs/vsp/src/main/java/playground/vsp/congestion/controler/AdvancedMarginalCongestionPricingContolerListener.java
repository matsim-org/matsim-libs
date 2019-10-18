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

package playground.vsp.congestion.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.congestion.handlers.AdvancedMarginalCongestionPricingHandler;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author ihab, amit
 *
 */

public class AdvancedMarginalCongestionPricingContolerListener implements StartupListener, AfterMobsimListener, IterationEndsListener {
	private final static Logger log = Logger.getLogger(AdvancedMarginalCongestionPricingContolerListener.class);

	private final Scenario scenario;
	private TollHandler tollHandler;
	private EventHandler congestionHandler;
	
	private double factor = 1.0;
	private AdvancedMarginalCongestionPricingHandler pricingHandler;
	
	/**
	 * @param scenario
	 * @param tollHandler
	 * @param handler must be one of the implementation for congestion pricing 
	 */
	public AdvancedMarginalCongestionPricingContolerListener(Scenario scenario, TollHandler tollHandler, EventHandler congestionHandler){
		this(scenario, tollHandler, congestionHandler, 1.0);
	}
	
	public AdvancedMarginalCongestionPricingContolerListener(Scenario scenario, TollHandler tollHandler, EventHandler congestionHandler, double factor){
		this.scenario = scenario;
		this.tollHandler = tollHandler;
		this.congestionHandler = congestionHandler;
		this.factor = factor;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getServices().getEvents();
		
		this.pricingHandler = new AdvancedMarginalCongestionPricingHandler(eventsManager, this.scenario, this.factor);
		
		eventsManager.addHandler(this.congestionHandler);
		eventsManager.addHandler(this.pricingHandler);
		eventsManager.addHandler(this.tollHandler);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		log.info("Monetize final congestion events...");
		this.pricingHandler.processFinalCongestionEvents();
		log.info("Total monetized amount: " + this.pricingHandler.getAmountSum());
		log.info("Monetize final congestion events... Done.");
		
		log.info("Writing out VTTS statistics...");
		this.pricingHandler.printVTTS(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/VTTS_forAllDelayedTrips.csv");
		this.pricingHandler.printAvgVTTSperPerson(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/VTTS_AvgPerPerson_forAllDelayedTrips.csv");
		log.info("Writing out VTTS statistics... Done.");
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
				
		log.info("Set average tolls for each link Id and time bin...");
		this.tollHandler.setLinkId2timeBin2avgToll();
		log.info("Set average tolls for each link Id and time bin... Done.");
		
		// write out analysis every iteration
		this.tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");
	}

}
