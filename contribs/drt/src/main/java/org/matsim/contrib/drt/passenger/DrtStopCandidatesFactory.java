/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.passenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.router.RoutedStopCandidates;
import org.matsim.contrib.dvrp.router.RoutedStopCandidates.Candidate;
import org.matsim.contrib.dvrp.router.StopCandidatesRoute;

import com.google.common.base.Preconditions;

/**
 * Builds the {@link DrtStopCandidates} of a request from the stop candidates routed into the {@link StopCandidatesRoute}s
 * of its passengers, so that the insertion search may choose another pickup/dropoff stop than the routed one.
 * <p>
 * The routed candidates list the preferred (routed) stop first; {@link DrtStopCandidates} holds only the alternatives,
 * plus the walk durations of the preferred stops, which are the request's own from/to links.
 *
 * @author nkuehnel / MOIA
 */
public final class DrtStopCandidatesFactory {
	private DrtStopCandidatesFactory() {
	}

	/**
	 * Single-person variant of {@link #fromRoutes(List, Network, boolean)}.
	 */
	public static Optional<DrtStopCandidates> fromRoute(Route route, Network network, boolean allowAlternativePickups) {
		return fromRoutes(List.of(route), network, allowAlternativePickups);
	}

	/**
	 * Combines the stop candidates of the routes of all passengers of one request. A stop is a candidate if it is a
	 * candidate of every passenger; its walk duration is the longest one among the passengers, since the group boards
	 * together. The order is that of the first route.
	 *
	 * @param allowAlternativePickups whether alternative pickup stops may be offered (false if a passenger may already be
	 *                                on the way to the routed stop)
	 * @return the candidates for a request with the routed (preferred) stops as its own from/to link, or empty if there
	 * are no common alternatives or a route has no candidates or does not start/end at its preferred stops
	 */
	public static Optional<DrtStopCandidates> fromRoutes(List<Route> routes, Network network,
			boolean allowAlternativePickups) {
		Preconditions.checkArgument(!routes.isEmpty(), "at least one route is required");
		List<RoutedStopCandidates> all = new ArrayList<>();
		for (Route route : routes) {
			if (!(route instanceof StopCandidatesRoute candidatesRoute)) {
				return Optional.empty();
			}
			Optional<RoutedStopCandidates> candidates = candidatesRoute.getStopCandidates();
			if (candidates.isEmpty()
					|| !candidates.get().preferredAccess().linkId().equals(route.getStartLinkId())
					|| !candidates.get().preferredEgress().linkId().equals(route.getEndLinkId())) {
				return Optional.empty(); // no candidates, or the route was changed after routing
			}
			all.add(candidates.get());
		}
		Id<Link> preferredAccess = all.getFirst().preferredAccess().linkId();
		Id<Link> preferredEgress = all.getFirst().preferredEgress().linkId();
		if (all.stream().anyMatch(c -> !c.preferredAccess().linkId().equals(preferredAccess)
				|| !c.preferredEgress().linkId().equals(preferredEgress))) {
			return Optional.empty(); // the group does not share its routed stops
		}

		List<DrtStopCandidate> access = allowAlternativePickups ?
				alternatives(sharedByAll(all.stream().map(RoutedStopCandidates::access).toList()), network) :
				List.of();
		List<DrtStopCandidate> egress = alternatives(
				sharedByAll(all.stream().map(RoutedStopCandidates::egress).toList()), network);
		if (access.isEmpty() && egress.isEmpty()) {
			return Optional.empty();
		}
		double accessWalk = all.stream().mapToDouble(c -> c.preferredAccess().walkDuration()).max().orElseThrow();
		double egressWalk = all.stream().mapToDouble(c -> c.preferredEgress().walkDuration()).max().orElseThrow();
		return Optional.of(new DrtStopCandidates(accessWalk, egressWalk, access, egress));
	}

	/**
	 * @return true if every route carries stop candidates with alternatives (cheap check before looking at the plans)
	 */
	public static boolean haveAlternatives(List<Route> routes) {
		return !routes.isEmpty() && routes.stream()
				.allMatch(route -> route instanceof StopCandidatesRoute candidatesRoute
						&& candidatesRoute.getStopCandidates().map(RoutedStopCandidates::hasAlternatives).orElse(false));
	}

	/**
	 * @param candidatesPerPassenger the access or the egress candidates of each passenger (preferred first)
	 * @return the candidates of the first passenger that all passengers share (preferred first), each with the longest
	 * walk among the passengers
	 */
	private static List<Candidate> sharedByAll(List<List<Candidate>> candidatesPerPassenger) {
		List<Candidate> shared = new ArrayList<>();
		for (Candidate candidate : candidatesPerPassenger.getFirst()) {
			double walkDuration = 0;
			double walkDistance = 0;
			boolean sharedByAll = true;
			for (List<Candidate> ofPassenger : candidatesPerPassenger) {
				Optional<Candidate> same = findByLink(ofPassenger, candidate.linkId());
				if (same.isEmpty()) {
					sharedByAll = false;
					break;
				}
				walkDuration = Math.max(walkDuration, same.get().walkDuration());
				walkDistance = Math.max(walkDistance, same.get().walkDistance());
			}
			if (sharedByAll) {
				shared.add(new Candidate(candidate.linkId(), candidate.coord(), walkDuration, walkDistance));
			}
		}
		return shared;
	}

	/**
	 * @return the first candidate on the given link (as the insertion search, which evaluates each link once)
	 */
	private static Optional<Candidate> findByLink(List<Candidate> candidates, Id<Link> linkId) {
		for (Candidate candidate : candidates) {
			if (candidate.linkId().equals(linkId)) {
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	/**
	 * @return the candidates without the first (preferred) one
	 */
	private static List<DrtStopCandidate> alternatives(List<Candidate> candidates, Network network) {
		return candidates.subList(1, candidates.size())
				.stream()
				.map(candidate -> new DrtStopCandidate(
						Preconditions.checkNotNull(network.getLinks().get(candidate.linkId()),
								"Stop candidate link %s not in network", candidate.linkId()),
						candidate.walkDuration()))
				.toList();
	}
}
