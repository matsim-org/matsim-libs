/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.casim.simulation.CANetsimEngine;

public class CAMultiLaneNetworkFactory implements CANetworkFactory {

	@Override
	public CANetwork createCANetwork(Network net, EventsManager em,
			CANetsimEngine engine) {
		return new CAMultiLaneNetwork(net, em, engine);
	}

	@Override
	public void setDensityEstimatorFactory(CASimDensityEstimatorFactory fac) {
		throw new RuntimeException("not implemented yet!");

	}

}
