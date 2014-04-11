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
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.analysis.extCost.ExtCostEventHandler;
import playground.ikaddoura.analysis.extCost.TripInfoWriter;

/**
 * @author ikaddoura
 *
 */

public class MarginalCostCalculation implements IterationEndsListener, IterationStartsListener {
	private static final Logger log = Logger.getLogger(MarginalCostCalculation.class);

	private final ScenarioImpl scenario;
	private MarginalCongestionHandlerImplV3 congestionHandler;
	private ExtCostEventHandler extCostHandler;
	private TollHandler tollHandler;
	
	public MarginalCostCalculation(ScenarioImpl scenario){
		this.scenario = scenario;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		if (event.getIteration() == this.scenario.getConfig().controler().getLastIteration()) {
			
			log.info("Computing congestion effects in the final iteration.");
			congestionHandler = new MarginalCongestionHandlerImplV3(event.getControler().getEvents(), scenario);
			event.getControler().getEvents().addHandler(congestionHandler);
			
			log.info("Analyzing congestion effects in the final iteration.");
			extCostHandler = new ExtCostEventHandler(this.scenario, false);
			event.getControler().getEvents().addHandler(extCostHandler);
			tollHandler = new TollHandler(scenario);
			event.getControler().getEvents().addHandler(tollHandler);			
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		if (event.getIteration() == this.scenario.getConfig().controler().getLastIteration()) {
			
			congestionHandler.writeCongestionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/congestionStats.csv");
			tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/tollStats.csv");
			
			TripInfoWriter writerCar = new TripInfoWriter(extCostHandler, event.getControler().getControlerIO().getIterationPath(event.getIteration()));
			writerCar.writeDetailedResults(TransportMode.car);
			writerCar.writeAvgTollPerDistance(TransportMode.car);
			writerCar.writeAvgTollPerTimeBin(TransportMode.car);
		}
	}

}
