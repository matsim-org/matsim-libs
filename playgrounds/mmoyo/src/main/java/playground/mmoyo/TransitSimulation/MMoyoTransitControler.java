/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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
package playground.mmoyo.TransitSimulation;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.queuesim.TransitQueueSimulation;

//import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
//import org.matsim.run.OTFVis;
import playground.mrieser.OTFDemo;
import playground.mrieser.pt.config.TransitConfigGroup;
import playground.mrieser.pt.controler.TransitControler;

//import org.matsim.ptproject.controller.PtController;


public class MMoyoTransitControler extends TransitControler {
	boolean launchOTFDemo=false;
	
	public MMoyoTransitControler(final String[] args) {
		super(args);
	}
	 
	public MMoyoTransitControler(final ScenarioImpl scenario, boolean launchOTFDemo){
		super(scenario);
		this.launchOTFDemo = launchOTFDemo;
	}
	
	@Override
	protected void runMobSim() {
		if (launchOTFDemo){
			TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
			sim.startOTFServer("livesim");
			OTFDemo.ptConnect("livesim");
			sim.run();

			/*
			TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
			sim.startOTFServer("livesim");
			new OnTheFlyClientQuad("rmi:127.0.0.1:4019:" + "livesim").start();
			sim.run();
			*/
		}
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		return new MMoyoPlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), this.scenarioData.getTransitSchedule(), new TransitConfigGroup());
	}
	
	public static void main(final String[] args) {
		if (args.length > 0) {
			new MMoyoTransitControler(args).run();
		} else {
			new MMoyoTransitControler(new String[] {"src/playground/mmoyo/demo/Berlin/BerlinConfig.xml"}).run();
		}
	}
	
}
