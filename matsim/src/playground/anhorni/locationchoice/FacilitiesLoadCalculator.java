/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice;

import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.controler.listener.StartupListener;

/**
 *  Basically it integrates the
 * {@link org.matsim.scoring.EventsToFacilityLoad} with the
 * {@link org.matsim.controler.Controler}.
 *
 * @author anhorni
 */
public class FacilitiesLoadCalculator implements StartupListener, AfterMobsimListener {

	private EventsToFacilityLoad facilityLoadCalculator;

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.facilityLoadCalculator = new EventsToFacilityLoad(controler.getFacilities());
		event.getControler().getEvents().addHandler(this.facilityLoadCalculator);
	}

	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		this.facilityLoadCalculator.reset(event.getIteration());
	}
}
