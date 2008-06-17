/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.router.util.TravelTimeI;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;

public class PlanomatControlerListener implements StartupListener, IterationStartsListener {

	private TravelTimeI linkTravelTimeCalculatorForPlanomat = null;
	private DepartureDelayAverageCalculator tDepDelayCalc = null;

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		NetworkLayer network = controler.getNetwork();
		this.linkTravelTimeCalculatorForPlanomat = initTravelTimeIForPlanomat(network);
		controler.getEvents().addHandler((EventHandlerI) this.linkTravelTimeCalculatorForPlanomat);

		int timeBinSize = 900;
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(network, timeBinSize);
		controler.getEvents().addHandler(this.tDepDelayCalc);
	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.tDepDelayCalc.reset(event.getIteration());
		((EventHandlerI) this.linkTravelTimeCalculatorForPlanomat).reset(event.getIteration());
	}

	private TravelTimeI initTravelTimeIForPlanomat(final NetworkLayer network) {

		TravelTimeI linkTravelTimeEstimator = null;

//		String travelTimeIName = Gbl.getConfig().planomat().getLinkTravelTimeEstimatorName();
//
//		if (travelTimeIName.equalsIgnoreCase("org.matsim.trafficmonitoring.TravelTimeCalculatorArray")) {
//			linkTravelTimeEstimator = new TravelTimeCalculatorArray(network);
//		} else if (travelTimeIName.equalsIgnoreCase("org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator")) {
//			linkTravelTimeEstimator = new LinearInterpolatingTTCalculator(network);
//		} else {
//			Gbl.errorMsg("Invalid name of implementation of TravelTimeI: " + travelTimeIName);
//		}

		return linkTravelTimeEstimator;
	}
}
