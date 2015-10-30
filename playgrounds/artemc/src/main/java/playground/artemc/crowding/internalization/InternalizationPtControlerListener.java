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

package playground.artemc.crowding.internalization;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;

/**
 * @author Ihab
 *
 */

public class InternalizationPtControlerListener implements StartupListener {

	private final MutableScenario scenario;
	private ScoreTracker scoreTracker;

	public InternalizationPtControlerListener(MutableScenario scenario, ScoreTracker scoreTracker){
		this.scenario = scenario;
		this.scoreTracker = scoreTracker;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();
		
		event.getControler().getEvents().addHandler(new TransferDelayInVehicleHandler(eventsManager, scenario));
		//event.getControler().getEvents().addHandler(new TransferDelayWaitingHandler(eventsManager, scenario));
		event.getControler().getEvents().addHandler(new CapacityDelayHandler(eventsManager, scenario));
		event.getControler().getEvents().addHandler(new MarginalCostPricingPtHandler(eventsManager, scenario, scoreTracker));
	}

}
