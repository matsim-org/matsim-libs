/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.dsim.Message;
import org.matsim.vehicles.Vehicle;

public class ComplexTransitStopHandlerFactory implements TransitStopHandlerFactory {

	@Override
	public TransitStopHandler createTransitStopHandler(Vehicle vehicle) {
		return new ComplexTransitStopHandler(vehicle);
	}

	@Override
	public TransitStopHandler createTransitStopHandler(Message message) {
		return new ComplexTransitStopHandler((ComplexTransitStopHandler.Msg) message);
	}

}
