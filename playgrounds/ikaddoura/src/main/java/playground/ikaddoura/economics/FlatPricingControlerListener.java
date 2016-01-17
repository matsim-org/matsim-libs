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

package playground.ikaddoura.economics;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;

/**
 * @author ikaddoura
 *
 */

public class FlatPricingControlerListener implements StartupListener {

	private final MutableScenario scenario;
	private PricingHandler flatPricingHandler;
	private double toll;

	public FlatPricingControlerListener(MutableScenario scenario, double toll){
		this.scenario = scenario;
		this.toll = toll;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getServices().getEvents();
		this.flatPricingHandler = new PricingHandler(eventsManager, scenario, toll);
		event.getServices().getEvents().addHandler(flatPricingHandler);
	}

}
