/* *********************************************************************** *
 * project: org.matsim.*
 * PreconfigureMultiModalControlerListener.java
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

package playground.christoph.evacuation.controler;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.router.util.PersonalizedTravelTimeFactory;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

/**
 * Configures the MultiModalControlerListener with additional TravelTimeFactories.
 * This is done before the MultiModalControlerListener has processed the StartupEvent.
 * 
 * This is only a workaround until the PT integration into the code as well as into the
 * model is done!
 * 
 * @author cdobler
 */
public class PreconfigureMultiModalControlerListener implements StartupListener {

	private final MultiModalControlerListener multiModalControlerListener;
	
	public PreconfigureMultiModalControlerListener(MultiModalControlerListener multiModalControlerListener) {
		this.multiModalControlerListener = multiModalControlerListener;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.multiModalControlerListener.addAdditionalTravelTimeFactory(TransportMode.pt, new PersonalizedTravelTimeFactory());
	}
	
}
