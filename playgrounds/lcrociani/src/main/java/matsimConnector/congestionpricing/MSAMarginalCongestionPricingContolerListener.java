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

package matsimConnector.congestionpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.congestion.analysis.CongestionAnalysisEventHandler;
import playground.vsp.congestion.analysis.CongestionAnalysisWriter;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;

/**
 * @author ihab, amit, glaemmel
 *
 */

public class MSAMarginalCongestionPricingContolerListener implements StartupListener, IterationEndsListener {
	private final Logger log = Logger.getLogger(MSAMarginalCongestionPricingContolerListener.class);

	private final MutableScenario scenario;
	private final MSATollHandler tollHandler;
	private final EventHandler congestionHandler;
	private MarginalCongestionPricingHandler pricingHandler;
	private CongestionAnalysisEventHandler extCostHandler;
	
	/**
	 * @param scenario
	 * @param tollHandler
	 * @param handler must be one of the implementation for congestion pricing 
	 */
	public MSAMarginalCongestionPricingContolerListener(Scenario scenario, MSATollHandler tollHandler, EventHandler congestionHandler){
		this.scenario =  (MutableScenario)scenario;
		this.tollHandler = tollHandler;
		this.congestionHandler = congestionHandler;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getServices().getEvents();
		
		this.pricingHandler = new MarginalCongestionPricingHandler(eventsManager, this.scenario);
		this.extCostHandler = new CongestionAnalysisEventHandler(this.scenario, true);
		
		eventsManager.addHandler(this.congestionHandler);
		eventsManager.addHandler(this.pricingHandler);
		eventsManager.addHandler(this.tollHandler);
		eventsManager.addHandler(this.extCostHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.log.info("Set average tolls for each link Id and time bin...");
		this.tollHandler.setLinkId2timeBin2avgToll();
		this.log.info("Set average tolls for each link Id and time bin... Done.");
		
		// write out analysis every iteration
		this.tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");
//		this.congestionHandler.writeCongestionStats(this.scenario.getConfig().services().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");
		CongestionAnalysisWriter writerCar = new CongestionAnalysisWriter(this.extCostHandler, event.getServices().getControlerIO().getIterationPath(event.getIteration()));
		writerCar.writeDetailedResults(TransportMode.car);
		writerCar.writeAvgTollPerDistance(TransportMode.car);
		writerCar.writeAvgTollPerTimeBin(TransportMode.car);
	}

}
