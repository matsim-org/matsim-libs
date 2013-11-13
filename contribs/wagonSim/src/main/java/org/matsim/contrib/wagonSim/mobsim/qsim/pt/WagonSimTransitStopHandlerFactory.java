/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.wagonSim.mobsim.qsim.pt;

import org.apache.log4j.Logger;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * @author droeder
 *
 */
public class WagonSimTransitStopHandlerFactory implements
		TransitStopHandlerFactory {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimTransitStopHandlerFactory.class);
	private WagonSimVehicleLoadListener vehicleLoadListener;
	private ObjectAttributes locomitiveAttribs;
	private ObjectAttributes wagonAttribs;

	public WagonSimTransitStopHandlerFactory(WagonSimVehicleLoadListener vehicleLoadListener, 
			ObjectAttributes wagonAttribs,
			ObjectAttributes locomotiveAttribs) {
		this.vehicleLoadListener = vehicleLoadListener;
		this.wagonAttribs = wagonAttribs;
		this.locomitiveAttribs = locomotiveAttribs;
	}

	@Override
	public TransitStopHandler createTransitStopHandler(Vehicle vehicle) {
		return new WagonSimTransitStopHandler(vehicle, 
				vehicleLoadListener.getCurrentLoad(), 
				wagonAttribs, 
				locomitiveAttribs);
	}
}

