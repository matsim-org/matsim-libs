/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerSTrehler2010Router
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
package playground.dgrether.koehlerstrehlersignal.conversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;


/**
 * @author dgrether
 *
 */
public class DgKS2010Router {

	private static final class SimpleTravelTimeDisutility implements TravelTime, TravelDisutility {

		private double calcTravelTime(Link link) {
			return link.getLength() / link.getFreespeed();
		}
		
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return this.calcTravelTime(link);
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return this.calcTravelTime(link);
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return this.calcTravelTime(link);
		}
		
	};

	private Person fakePerson = new Person(){
		@Override
		public Id getId() {
			return null;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return null;
		}

		@Override
		public List<? extends Plan> getPlans() {
			return null;
		}

		@Override
		public void setId(Id id) {
		}

		@Override
		public boolean addPlan(Plan p) {
			return false;
		}

		@Override
		public Plan getSelectedPlan() {
			return null;
		}
		
	};
	
	private Vehicle fakeVehicle = new Vehicle(){
		@Override
		public Id getId() {
			return null;
		}
		@Override
		public VehicleType getType() {
			return null;
		}
	};

	private static final Logger log = Logger.getLogger(DgKS2010Router.class);
	
	
	public List<Id> routeCommodities(Network network, DgCommodities commodities) {
		SimpleTravelTimeDisutility travelTime = new SimpleTravelTimeDisutility();
		Dijkstra dijkstra = new Dijkstra(network, travelTime, travelTime);
		List<Id> invalidCommodities = new ArrayList<Id>();
		for (DgCommodity commodity : commodities.getCommodities().values()){
			Node fromNode = network.getNodes().get(commodity.getSourceNode());
			Node toNode = network.getNodes().get(commodity.getDrainNodeId());
			log.info("Searching path from,to node " + fromNode.getId() + "," + toNode.getId());
			Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 1.0, fakePerson, fakeVehicle); //call for dijkstra
			log.info("Found path for commodity id " + commodity.getId() + " " + path);
			if (path == null) {
				invalidCommodities.add(commodity.getId());
			}
		}
		return invalidCommodities;
	}
	
	
}
