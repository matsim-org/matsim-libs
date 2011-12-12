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
package org.matsim.contrib.freight.vrp.algorithms.rr.recreation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.RecreationStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Solution;
import org.matsim.contrib.freight.vrp.api.Offer;
import org.matsim.contrib.freight.vrp.api.SingleDepotVRP;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;



/**
 * Simplest recreation strategy. All removed customers are inserted where insertion costs are minimal. I.e. each tour-agent is asked for
 * minimal marginal insertion costs. The tour-agent offering the lowest marginal insertion costs gets the customer/shipment.
 * 
 * @author stefan schroeder
 *
 */

public class BestInsertion implements RecreationStrategy{

	private Logger logger = Logger.getLogger(BestInsertion.class);
	
	private SingleDepotVRP vrp;
	
	private TourAgentFactory tourAgentFactory;
	
	private Collection<RecreationListener> recreationListeners = new ArrayList<RecreationListener>();
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

	public Collection<RecreationListener> getRecreationListener() {
		return recreationListeners;
	}

	public BestInsertion(SingleDepotVRP vrp) {
		super();
		this.vrp = vrp;
	}

	public void setTourAgentFactory(TourAgentFactory tourAgentFactory) {
		this.tourAgentFactory = tourAgentFactory;
	}

	@Override
	public void run(Solution tentativeSolution, List<Shipment> shipmentsWithoutService) {
		Collections.shuffle(shipmentsWithoutService,random);
		for(Shipment shipmentWithoutService : shipmentsWithoutService){
			assertShipmentIsInCorrectOrder(shipmentWithoutService);
			Offer bestOffer = null;
			for(TourAgent agent : tentativeSolution.getTourAgents()){
				double bestKnownPrice;
				if(bestOffer == null){
					bestKnownPrice = Double.MAX_VALUE;
				}
				else{
					bestKnownPrice = bestOffer.getPrice();
				}
				Offer offer = agent.requestService(shipmentWithoutService,bestKnownPrice);
				if(offer == null){
					continue;
				}
				if(bestOffer == null){
					bestOffer = offer;
				}
				else if(offer.getPrice() < bestOffer.getPrice()){
					bestOffer = offer;
				}
			}
			if(bestOffer != null){
				logger.debug("offer granted " + bestOffer.getServiceProvider() + " " + bestOffer + " " + shipmentWithoutService);
				bestOffer.getServiceProvider().offerGranted(shipmentWithoutService);
				informListeners(shipmentWithoutService,bestOffer.getPrice());
			}
			else{
				TourAgent newTourAgent = createTourAgent(shipmentWithoutService);
				if(newTourAgent.tourIsValid()){
					tentativeSolution.getTourAgents().add(newTourAgent);
				}
				else{
					throw new IllegalStateException("could not create a valid round-tour" + newTourAgent);
				}
				
			}
		}
	}

	

	private void assertShipmentIsInCorrectOrder(Shipment shipmentWithoutService) {
		if(shipmentWithoutService.getFrom().getDemand() < 0){
			throw new IllegalStateException("this must be a pickup and can thus not be smaller than 0. " + shipmentWithoutService.getFrom().getId() + "; " + shipmentWithoutService.getTo().getId());
		}
		
	}

	private void informListeners(Shipment shipment, double cost) {
		for(RecreationListener l : recreationListeners){
			l.inform(new RecreationEvent(shipment,cost));
		}
		
	}

	private TourAgent createTourAgent(Shipment shipmentWithoutService) {
		Tour tour = VrpUtils.createRoundTour(vrp, shipmentWithoutService.getFrom(), shipmentWithoutService.getTo());
		Vehicle vehicle = VrpUtils.createVehicle(vrp.getVehicleType());
		TourAgent agent = tourAgentFactory.createTourAgent(tour, vehicle);
		return agent;
	}

	@Override
	public void addListener(RecreationListener l) {
		recreationListeners.add(l);	
	}

}
