/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleEventHandler.java
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

import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * @author benjamin
 *
 */
public class VehicleEventHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
