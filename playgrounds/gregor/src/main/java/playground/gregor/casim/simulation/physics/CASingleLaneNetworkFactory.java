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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.casim.simulation.CANetsimEngine;

public class CASingleLaneNetworkFactory implements CANetworkFactory {

	private static final Logger log = Logger
			.getLogger(CASingleLaneNetworkFactory.class);

	private CASimDensityEstimatorFactory fac = new CASingleLaneDensityEstimatorSPAFactory();

	@Override
	public CANetwork createCANetwork(Network net, EventsManager em,
			CANetsimEngine engine) {
		return new CASingleLaneNetwork(net, em, engine, this.fac);
	}

	@Override
	public void setDensityEstimatorFactory(CASimDensityEstimatorFactory fac) {
		if (!(fac instanceof CASingleLaneDensityEstimatorSPAFactory)
				&& !(fac instanceof CASingleLaneDensityEstimatorSPHFactory)) {
			log.warn(CASimDensityEstimatorFactory.class.toString()
					+ " of type:" + fac.getClass().toString()
					+ " is not allowd here! Ignored!");
			return;
		}
		this.fac = fac;

	}

}
