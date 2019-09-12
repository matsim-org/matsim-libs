/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.pendlermatrix;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class RouterFilter implements TripFlowSink {

	private Network network;

	private LeastCostPathCalculator dijkstra;

	private TripFlowSink sink;

	private Collection<Id> interestingNodeIds = new HashSet<Id>();
	
	private double travelTimeToLink = 0.0;
	
	private Coord entryCoord;
	
	private final Scenario sc ;

	public RouterFilter(Network network) {
		this.network = network;
		FreespeedTravelTimeAndDisutility fttc = new FreespeedTravelTimeAndDisutility(new PlanCalcScoreConfigGroup());
		dijkstra = new DijkstraFactory().createPathCalculator(network, fttc, fttc);
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
	}

	@Override
	public void process(ActivityFacility quelle, ActivityFacility ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset) {
		Node quellNode = NetworkUtils.getNearestNode(((Network) network),quelle.getCoord());
		Node zielNode = NetworkUtils.getNearestNode(((Network) network),ziel.getCoord());
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0, null, null);
		ActivityFacilitiesFactory factory = ((MutableScenario)sc).getActivityFacilities().getFactory() ;
		if (isInteresting(path)) {
//			Facility newQuelle = new Zone(quelle.getId(), quelle.workplaces, quelle.workingPopulation, entryCoord);
			ActivityFacility newQuelle = factory.createActivityFacility(quelle.getId(), quelle.getCoord() ) ;
			for ( ActivityOption option : quelle.getActivityOptions().values() ) { 
				newQuelle.addActivityOption(option) ;
			}

			sink.process(newQuelle, ziel, quantity, mode, destinationActivityType, departureTimeOffset + travelTimeToLink);
		}
	}

	private boolean isInteresting(Path path) {
		if (interestingNodeIds.contains(path.getFromNode())) {
			if (interestingNodeIds.contains(path.getToNode())) {
				return false;
			}
		}
		for (Node node : path.nodes) {
			if (interestingNodeIds.contains(node.getId())) {
				entryCoord = node.getCoord();
				travelTimeToLink = calculateFreespeedTravelTimeToNode(network, path, node);
				return true;
			}
		}
		return false;
	}

	void setSink(TripFlowSink sink) {
		this.sink = sink;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	Collection<Id> getInterestingNodeIds() {
		return interestingNodeIds;
	}

	private static double calculateFreespeedTravelTimeToNode(Network network, Path path, Node node) {
		double travelTime = 0.0;
		for (Link l : path.links) {
			if (l.getFromNode().equals(node)) {
				return travelTime;
			}
			travelTime += l.getLength() / l.getFreespeed();
			if (l.getToNode().equals(node)) {
				return travelTime;
			}
		}
		return travelTime;
	}

}
