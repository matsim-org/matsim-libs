/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package contrib.multimodal;

import java.util.Map;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;

import contrib.multimodal.router.MultimodalTripRouterFactory;
import contrib.multimodal.router.util.MultiModalTravelTimeFactory;

public class MultiModalControlerListener implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {

		Controler controler = event.getControler();
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(event.getControler().getConfig());
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		MultimodalTripRouterFactory tripRouterFactory = new MultimodalTripRouterFactory(
				controler, multiModalTravelTimes);
		MultimodalQSimFactory qSimFactory = new MultimodalQSimFactory(multiModalTravelTimes);
		controler.setTripRouterFactory(tripRouterFactory);
		controler.setMobsimFactory(qSimFactory);
	}	
}