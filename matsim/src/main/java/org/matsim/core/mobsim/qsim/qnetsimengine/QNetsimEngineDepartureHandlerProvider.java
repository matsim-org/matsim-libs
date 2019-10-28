
/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngineDepartureHandlerProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;
import javax.inject.Provider;

class QNetsimEngineDepartureHandlerProvider implements Provider<VehicularDepartureHandler> {
	// yyyyyy should return an interface.  Otherwise one must inherit from the implementation, which we don't like! kai, may'18

	@Inject
	QNetsimEngine qNetsimEngine;

	@Override
	public VehicularDepartureHandler get() {
		return qNetsimEngine.getDepartureHandler();
	}
}
