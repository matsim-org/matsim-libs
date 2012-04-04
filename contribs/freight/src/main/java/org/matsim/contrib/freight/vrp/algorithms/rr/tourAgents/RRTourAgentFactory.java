/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.CostParams;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;



/**
 * 
 * @author stefan schroeder
 *
 */

public class RRTourAgentFactory {

	private TourStatusProcessor tourStatusProcessor;
	
	private TourFactory tourBuilder;
	
	private CostParams costParams;
	
	public RRTourAgentFactory(TourStatusProcessor tourStatusProcessor, TourFactory tourBuilder, CostParams costParams) {
		super();
		this.tourStatusProcessor = tourStatusProcessor;
		this.tourBuilder = tourBuilder;
		this.costParams = costParams;
	}

	public RRTourAgent createTourAgent(Tour tour, Vehicle vehicle) {
		RRTourAgent tourAgent = new RRTourAgent(vehicle, tour, tourStatusProcessor, tourBuilder, costParams);
		tourAgent.marginalCostScalingFactorForNewService = 5.0;
		tourAgent.fixCostsForService = 100.0;
		return tourAgent;
	}
}
