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
package org.matsim.contrib.freight.vrp.basics;


/**
 * 
 * @author stefan schroeder
 *
 */

public class ManhattanCosts implements Costs {

	public double speed = 1;
	
	private Locations locations;
	
	private DriverCostParams costParams = new DriverCostParams(1.0,1.0,0.0,0.0,0.0,100.0);
		
	public ManhattanCosts(Locations locations) {
		super();
		this.locations = locations;
	}

	@Override
	public Double getTransportCost(String fromId, String toId, double time) {
		return costParams.transportCost_per_meter*calculateDistance(fromId, toId);
	}

	@Override
	public Double getTransportTime(String fromId, String toId, double time) {
		double transportTime = calculateDistance(fromId, toId)/speed;
		return transportTime;
	}
	
	private double calculateDistance(String fromId, String toId){
		double distance = Math.abs(locations.getCoord(fromId).getX() - locations.getCoord(toId).getX()) + 
			Math.abs(locations.getCoord(fromId).getY() - locations.getCoord(toId).getY());
		return distance;
	}

	@Override
	public Double getBackwardTransportCost(String fromId, String toId,double arrivalTime) {
		return getTransportCost(fromId, toId, arrivalTime);
	}

	@Override
	public Double getBackwardTransportTime(String fromId, String toId,double arrivalTime) {
		return getTransportTime(fromId, toId, arrivalTime);
	}

	@Override
	public DriverCostParams getCostParams() {
		return costParams;
	}
}
