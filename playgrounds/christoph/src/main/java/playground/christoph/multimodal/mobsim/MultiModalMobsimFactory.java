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

package playground.christoph.multimodal.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.router.util.TravelTime;
import org.matsim.ptproject.qsim.ParallelQSimFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.ptproject.qsim.interfaces.QNetworkI;
import org.matsim.ptproject.qsim.netsimengine.DefaultQNetworkFactory;

import playground.christoph.multimodal.mobsim.netsimengine.MultiModalDepartureHandler;
import playground.christoph.multimodal.mobsim.netsimengine.MultiModalQNetworkFactory;
import playground.christoph.multimodal.router.costcalculator.BufferedTravelTime;
import playground.christoph.multimodal.router.costcalculator.MultiModalTravelTimeCost;
import playground.christoph.multimodal.router.costcalculator.TravelTimeCalculatorWithBuffer;

public class MultiModalMobsimFactory implements MobsimFactory {

	private static final Logger log = Logger.getLogger(MultiModalMobsimFactory.class);
	
	protected TravelTime travelTime;
	protected MobsimFactory mobSimFactory = new QSimFactory();
	protected MobsimFactory parallelMobSimFactory = new ParallelQSimFactory();
	
	/*
	 * The TravelTimeCalculator which is used by the Controler to reschedule the Plans. 
	 */
	public MultiModalMobsimFactory(TravelTime travelTime) {
		this.travelTime = travelTime;
	}
	
	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
		
		Simulation sim;
		
		// Get number of parallel Threads
		QSimConfigGroup conf = (QSimConfigGroup) sc.getConfig().getModule(QSimConfigGroup.GROUP_NAME);
		int numOfThreads = conf.getNumberOfThreads();

		/*
		 * Depending on the number of Threads create a QSim or a ParallelQSim using
		 * the existing SimFactories.
		 */
		if (numOfThreads > 1) {
			sim = parallelMobSimFactory.createMobsim(sc, eventsManager);
		}
		else {
			sim = mobSimFactory.createMobsim(sc, eventsManager);
		}
		
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
		} else log.warn("No BufferedTravelTime Object could be created. Using FreeSpeedTravelTimes instead.");
		
		/*
		 *  replace QNetwork
		 *  This is a bit of a hack - in my opinion it should be possible to
		 *  set the QNetworkFactory in the QSim.
		 */
		log.info("Replacing QNetwork...");
		QNetworkI network = DefaultQNetworkFactory.createQNetwork((QSim)sim, new MultiModalQNetworkFactory(multiModalTravelTime));
		// yyyy It is, I have to admint, not clear to me why this works, since the network will know the correct
		// qsim, but the qsim will not know the correct network (or will it???).  kai, aug'10
		// Well, the qSimEngine will have to correct network.  Weird.  kai, aug'10

		// then tell the QNetwork to use the simEngine (this also creates qlinks and qnodes)
		network.initialize(((QSim)sim).getQSimEngine());
		
		/*
		 *  add MultiModalDepartureHandler
		 */
		((QSim)sim).addDepartureHandler(new MultiModalDepartureHandler((QSim)sim));
		
	    return sim;
	}
}
