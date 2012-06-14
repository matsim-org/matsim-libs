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



/**
 * @author stefan schroeder
 *
 */
public class CrowFlyCosts implements Costs{
	
	private static Logger logger = Logger.getLogger(CrowFlyCosts.class);
	
	public int speed = 1;
	
	public double detourFactor = 1.0;
	
	private Locations locations;
	
	private DriverCostParams costParams = new DriverCostParams(1.0,1.0,0.0,0.0,0.0,1000.0); 
		
	public CrowFlyCosts(Locations locations) {
		super();
		this.locations = locations;
	}

	@Override
	public Double getTransportCost(String fromId, String toId, double time) {
		Double dist;
		try{
			dist = EuclideanDistanceCalculator.calculateDistance(locations.getCoord(fromId), locations.getCoord(toId))*detourFactor;
		}
		catch(NullPointerException e){
			logger.debug(fromId + " " + toId + " no dist found");
			throw new NullPointerException();
		}
		return costParams.transportCost_per_meter*dist; 
	}

	@Override
	public Double getTransportTime(String fromId, String toId, double time) {
		return getTransportCost(fromId, toId, 0.0)/speed;
	}

	@Override
	public Double getBackwardTransportCost(String fromId, String toId, double arrivalTime) {
		return getTransportCost(fromId, toId, arrivalTime);
	}

	@Override
	public Double getBackwardTransportTime(String fromId, String toId, double arrivalTime) {
		return getTransportTime(fromId, toId, arrivalTime);
	}

	@Override
	public DriverCostParams getCostParams() {
		return costParams;
	}

}
