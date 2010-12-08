/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleControlerListener.java
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
package playground.benjamin.szenarios.munich.testroad;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author benjamin
 *
 */
public class VehicleControlerListener implements StartupListener {

	private VehicleEventHandler vehicleEventHandler;
	
	public VehicleControlerListener(Scenario scenario) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
	}

}
