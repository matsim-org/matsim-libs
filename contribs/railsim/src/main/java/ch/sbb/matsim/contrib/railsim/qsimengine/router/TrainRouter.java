/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine.router;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * Calculates unblocked route between two {@link RailLink}.
 */
public final class TrainRouter {

	private final Network network;
	private final RailResourceManager resources;
	private final LeastCostPathCalculator lpc;

	private final DisUtility disutility = new DisUtility();

	@Inject
	public TrainRouter(QSim qsim, RailResourceManager resources) {
		this(qsim.getScenario().getNetwork(), resources);
	}

	public TrainRouter(Network network, RailResourceManager resources) {
		this.network = network;
		this.resources = resources;

		// uses the full network, which should not be slower than filtered network as long as dijkstra is used
		this.lpc = new DijkstraFactory().createPathCalculator(network, disutility, new FreeSpeedTravelTime());
	}

	/**
	 * Calculate the shortest path between two links. This method is not thread-safe, because of mutable state in the disutility.
	 */
	public List<RailLink> calcRoute(TrainPosition position, RailLink from, RailLink to) {

		Node fromNode = network.getLinks().get(from.getLinkId()).getToNode();
		Node toNode = network.getLinks().get(to.getLinkId()).getFromNode();

		disutility.setPosition(position);

		LeastCostPathCalculator.Path path = lpc.calcLeastCostPath(fromNode, toNode, 0, null, null);

		return path.links.stream().map(l -> resources.getLink(l.getId())).toList();
	}

	private final class DisUtility implements TravelDisutility {

		private TrainPosition position;

		public void setPosition(TrainPosition position) {
			this.position = position;
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			// only works with fixed block
			int weight = resources.hasCapacity(time, link.getId(), RailResourceManager.ANY_TRACK, position) ? 0 : 1;

			// Small offset in the weight prevents dead-locks in case there are loops within the station
			return weight + 0.00001;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}
	}
}
