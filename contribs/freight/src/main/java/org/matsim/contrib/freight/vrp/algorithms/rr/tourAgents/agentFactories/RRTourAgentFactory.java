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
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.agentFactories;

import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.OfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.ServiceProviderFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourStatusProcessor;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.DriverCostParams;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;



/**
 * 
 * @author stefan schroeder
 *
 */

public class RRTourAgentFactory implements ServiceProviderFactory{

	private TourStatusProcessor tourStatusProcessor;
	
	private DriverCostParams carrierCostParams;
	
	private OfferMaker offerMaker;
	
	public RRTourAgentFactory(TourStatusProcessor tourStatusProcessor, DriverCostParams carrierCostParams, OfferMaker offerMaker) {
		super();
		this.tourStatusProcessor = tourStatusProcessor;
		this.carrierCostParams = carrierCostParams;
		this.offerMaker = offerMaker;
	}

	public ServiceProviderAgent createAgent(Tour tour, Vehicle vehicle, Costs costs) {
		RRDriverAgent tourAgent = new RRDriverAgent(vehicle, tour, tourStatusProcessor, carrierCostParams);
		tourAgent.setOfferMaker(offerMaker);
		return tourAgent;
	}
}
