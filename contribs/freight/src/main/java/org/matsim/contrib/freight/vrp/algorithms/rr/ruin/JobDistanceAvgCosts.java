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
package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;

import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public class JobDistanceAvgCosts implements JobDistance {

	private VehicleRoutingCosts costs;

	public JobDistanceAvgCosts(VehicleRoutingCosts costs) {
		super();
		this.costs = costs;

	}

	@Override
	public double calculateDistance(Job i, Job j) {
		double avgCost = 0.0;
		if (i instanceof Shipment && j instanceof Shipment) {
			if (i.equals(j)) {
				avgCost = 0.0;
			} else {
				Shipment s_i = (Shipment) i;
				Shipment s_j = (Shipment) j;
				double cost_i1_j1 = calcDist(s_i.getFromId(), s_j.getFromId());
				double cost_i1_j2 = calcDist(s_i.getFromId(), s_j.getToId());
				double cost_i2_j1 = calcDist(s_i.getToId(), s_j.getFromId());
				double cost_i2_j2 = calcDist(s_i.getToId(), s_j.getToId());
				avgCost = (cost_i1_j1 + cost_i1_j2 + cost_i2_j1 + cost_i2_j2) / 4;
			}
		} else if (i instanceof Service && j instanceof Service) {
			if (i.equals(j)) {
				avgCost = 0.0;
			} else {
				Service s_i = (Service) i;
				Service s_j = (Service) j;
				avgCost = calcDist(s_i.getLocationId(), s_j.getLocationId());
			}
		} else {
			throw new UnsupportedOperationException(
					"currently, this class just works with shipments. when working with services "
							+ " also, implement another JobDistance");
		}
		return avgCost;
	}

	private double calcDist(String from, String to) {
		return costs.getTransportCost(from, to, 0.0, null, null);
	}

}
