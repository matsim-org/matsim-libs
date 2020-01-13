/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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
package playground.vsp.parkAndRide.example;


import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.vsp.parkAndRide.PRAdaptiveCapacityControl;


/**
 * 
 * @author ikaddoura
 *
 */
public class PRControlerListener implements StartupListener {
	
	private PRAdaptiveCapacityControl adaptiveControl;
	private MatsimServices controler;
	
	PRControlerListener(MatsimServices controler, PRAdaptiveCapacityControl adaptiveControl) {
		this.adaptiveControl = adaptiveControl;
		this.controler = controler;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		controler.getEvents().addHandler(adaptiveControl);
	}

}
