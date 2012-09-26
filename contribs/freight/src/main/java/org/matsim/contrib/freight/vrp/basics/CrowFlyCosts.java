/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.matsim.contrib.freight.vrp.basics;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.utils.EuclideanDistanceCalculator;

/**
 * @author stefan schroeder
 * 
 */
public class CrowFlyCosts implements VehicleRoutingCosts {

	private static Logger logger = Logger.getLogger(CrowFlyCosts.class);

	public int speed = 1;

	public double detourFactor = 1.0;

	private Locations locations;

	public CrowFlyCosts(Locations locations) {
		super();
		this.locations = locations;
	}

	@Override
	public double getTransportCost(String fromId, String toId, double time,
			Driver driver, Vehicle vehicle) {
		Double dist;
		try {
			dist = EuclideanDistanceCalculator.calculateDistance(
					locations.getCoord(fromId), locations.getCoord(toId))
					* detourFactor;
		} catch (NullPointerException e) {
			logger.debug(fromId + " " + toId + " no dist found");
			throw new NullPointerException();
		}
		return dist;
	}

	@Override
	public double getTransportTime(String fromId, String toId, double time,
			Driver driver, Vehicle vehicle) {
		return getTransportCost(fromId, toId, 0.0, null, null) / speed;
	}

	@Override
	public double getBackwardTransportCost(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle) {
		return getTransportCost(fromId, toId, arrivalTime, null, null);
	}

	@Override
	public double getBackwardTransportTime(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle) {
		return getTransportTime(fromId, toId, arrivalTime, null, null);
	}

}
