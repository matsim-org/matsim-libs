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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author Ihab
 *
 */

public class AverageCongestionPricingControlerListener implements StartupListener, AfterMobsimListener {
	private static final Logger log = Logger.getLogger(AverageCongestionPricingControlerListener.class);

	private final MutableScenario scenario;
	private TollHandler tollHandler;
	private CongestionHandlerImplV3 congestionHandler;
	
	public AverageCongestionPricingControlerListener(MutableScenario scenario, TollHandler tollHandler){
		this.scenario = scenario;
		this.tollHandler = tollHandler;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getServices().getEvents();
		congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
		
		event.getServices().getEvents().addHandler(congestionHandler);
		event.getServices().getEvents().addHandler(tollHandler);

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		log.info("Set average tolls for each link Id and time bin.");
		tollHandler.setLinkId2timeBin2avgToll();
		
		// write out toll statistics every iteration
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");
		
		// write out congestion statistics every iteration
		congestionHandler.writeCongestionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");
		
		log.info("Throwing agent money events based on calculated average marginal cost for each link and time bin.");
		EventsManager events = event.getServices().getEvents();
		
		for (LinkEnterEvent enterEvent : this.tollHandler.getLinkEnterEvents()) {
			double amount = tollHandler.getAvgToll(enterEvent.getLinkId(), enterEvent.getTime());
			PersonMoneyEvent moneyEvent = new PersonMoneyEvent(enterEvent.getTime(), Id.createPersonId(enterEvent.getVehicleId()), amount);
			events.processEvent(moneyEvent);
		}
	}

}
