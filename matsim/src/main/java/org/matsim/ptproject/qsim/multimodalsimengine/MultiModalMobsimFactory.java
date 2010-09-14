/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalMobsimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.multimodalsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.router.util.TravelTime;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.BufferedTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.MultiModalTravelTimeCost;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.TravelTimeCalculatorWithBuffer;

public class MultiModalMobsimFactory implements MobsimFactory {

	private static final Logger log = Logger.getLogger(MultiModalMobsimFactory.class);
	
	private MobsimFactory delegate;
	private TravelTime travelTime;
	
	public MultiModalMobsimFactory(MobsimFactory mobsimFactory,  TravelTime travelTime) {
		this.delegate = mobsimFactory;
		this.travelTime = travelTime;
	}
	
	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
		
		Simulation sim = delegate.createMobsim(sc, eventsManager);
		
		if (sim instanceof QSim) {
			log.info("Using MultiModalMobsim...");
			
			QSim qSim = (QSim) sim;
			
			/*
			 * Create a MultiModalTravelTime Calculator. It is passed over the the MultiModalQNetwork which
			 * needs it to estimate the TravelTimes of the NonCarModes.
			 * If the Controler uses a TravelTimeCalculatorWithBuffer (which is strongly recommended), a
			 * BufferedTravelTime Object is created and set as TravelTimeCalculator in the MultiModalTravelTimeCost
			 * Object.
			 */
			MultiModalTravelTimeCost multiModalTravelTime = new MultiModalTravelTimeCost(sc.getConfig().plansCalcRoute());
			
			if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
				BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
				bufferedTravelTime.setScaleFactor(1.25);
				multiModalTravelTime.setTravelTime(bufferedTravelTime);
			} else {
				log.warn("TravelTime is not instance of TravelTimeCalculatorWithBuffer!");
				log.warn("No BufferedTravelTime Object could be created. Using FreeSpeedTravelTimes instead.");
			}
			
			MultiModalSimEngine multiModalSimEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, multiModalTravelTime);
						
			// set MultiModalSimEngine
			qSim.setMultiModalSimEngine(multiModalSimEngine);
						
			// add MultiModalDepartureHandler
			qSim.addDepartureHandler(new MultiModalDepartureHandler(qSim, multiModalSimEngine));
		}
		else {
			log.error("Simulation Object is not from type QSim - cannot use MultiModalMobsim!");
		}
		
		return sim;
	}

}
