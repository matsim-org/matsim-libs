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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.vsp.pipeline.PersonSink;
import playground.vsp.pipeline.PersonSinkSource;

public class PersonRouterFilter implements PersonSinkSource {
	
private Network network;
	
	private LeastCostPathCalculator dijkstra;
	
	private PersonSink sink;
	
	private Collection<Id> interestingNodeIds = new HashSet<Id>();
	
	double travelTimeToEntry = 0.0;
	
	public PersonRouterFilter(Network network) {
		this.network = network;
		FreespeedTravelTimeAndDisutility fttc = new FreespeedTravelTimeAndDisutility(new PlanCalcScoreConfigGroup());
		dijkstra = new DijkstraFactory().createPathCalculator(network, fttc, fttc);
	}

	private boolean isInteresting(Path path) {
		for (Node node : path.nodes) {
			if (interestingNodeIds.contains(node.getId())) {
				travelTimeToEntry = calculateFreespeedTravelTimeToNode(network, path, node);
				return true;
			}
		}
		return false;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	Collection<Id> getInterestingNodeIds() {
		return interestingNodeIds;
	}

	@Override
	public void process(Person person) {
		Activity origin = (Activity) person.getPlans().get(0).getPlanElements().get(0);
		Activity destination = (Activity) person.getPlans().get(0).getPlanElements().get(2);
		Coord quelle = origin.getCoord();
		final Coord coord = quelle;
		Node quellNode = NetworkUtils.getNearestNode(((Network) network),coord);
		Coord ziel = destination.getCoord();
		final Coord coord1 = ziel;
		Node zielNode = NetworkUtils.getNearestNode(((Network) network),coord1);
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0, person, null);
		if (isInteresting(path)) {
			origin.setEndTime(origin.getEndTime() + travelTimeToEntry);
			sink.process(person);
		}
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
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
