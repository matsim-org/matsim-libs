/* *********************************************************************** *
 * project: org.matsim.*
 * NetsimWrappingQVehicleProvider.java
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
package org.matsim.contrib.socnetsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * @author thibautd
 */
public class NetsimWrappingQVehicleProvider implements QVehicleProvider {
	private final QNetsimEngine netsim;

	public NetsimWrappingQVehicleProvider(
			final QNetsimEngine netsim) {
		this.netsim = netsim;
	}

	@Override
	public QVehicle getVehicle(final Id id) {
		return netsim.getVehicles().get( id );
	}
}

