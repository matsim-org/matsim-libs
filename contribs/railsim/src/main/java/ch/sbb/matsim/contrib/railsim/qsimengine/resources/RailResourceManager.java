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

package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainManager;
import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.DeadlockAvoidance;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * Class responsible for managing and blocking resources and segments of links.
 */
public final class RailResourceManager {

	/**
	 * Constant that can be used as track number to indicate that any track is allowed.
	 */
	public static final int ANY_TRACK = -1;

	/**
	 * Constant to indicate that any track is allowed as long as the opposing direction is not blocked.
	 */
	public static final int ANY_TRACK_NON_BLOCKING = -2;

	private final EventsManager eventsManager;

	/**
	 * Rail links.
	 */
	private final Map<Id<Link>, RailLink> links;

	private final Map<Id<RailResource>, RailResource> resources;

	private final DeadlockAvoidance dla;
	private final TrainManager trains;

	/**
	 * Retrieve source id of a link.
	 */
	public static Id<RailResource> getResourceId(Link link) {
		String id = RailsimUtils.getResourceId(link);
		if (id == null)
			return Id.create(link.getId().toString(), RailResource.class);
		else
			return Id.create(id, RailResource.class);
	}

	@Inject
	public RailResourceManager(QSim qsim, DeadlockAvoidance dla, TrainManager trainManager) {
		this(qsim.getEventsManager(),
			ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), RailsimConfigGroup.class),
			qsim.getScenario().getNetwork(),
			dla, trainManager);
	}

	/**
	 * Construct resources from network.
	 */
	public RailResourceManager(EventsManager eventsManager, RailsimConfigGroup config,
							   Network network, DeadlockAvoidance dla, TrainManager trainManager) {
		this.eventsManager = eventsManager;
		this.dla = dla;
		this.trains = trainManager;
		this.links = new IdMap<>(Link.class, network.getLinks().size());

		// Mapping for resources to be created
		Map<Id<RailResource>, List<RailLink>> resourceMapping = new HashMap<>();

		Set<String> modes = config.getNetworkModes();
		for (Map.Entry<Id<Link>, ? extends Link> e : network.getLinks().entrySet()) {
			if (e.getValue().getAllowedModes().stream().anyMatch(modes::contains)) {

				Link opposite = NetworkUtils.findLinkInOppositeDirection(e.getValue());
				DisallowedNextLinks disallowed = NetworkUtils.getDisallowedNextLinks(e.getValue());

				Set<Id<Link>> disallowedNextLinks = null;
				if (disallowed != null) {
					disallowedNextLinks = new LinkedHashSet<>();
					for (String mode : config.getNetworkModes()) {
						List<List<Id<Link>>> sequences = disallowed.getDisallowedLinkSequences(mode);

						for (List<Id<Link>> sequence : sequences) {
							if (sequence.size() > 1)
								throw new IllegalArgumentException("Only disallowed sequences of length 1 are supported.");

							disallowedNextLinks.add(sequence.getFirst());
						}
					}
				}

				RailLink link = new RailLink(e.getValue(), opposite, disallowedNextLinks);

				if (link.length <= 0)
					throw new IllegalArgumentException("Link length must be greater than zero: " + link);

				resourceMapping.computeIfAbsent(getResourceId(e.getValue()), k -> new ArrayList<>()).add(link);
				this.links.put(e.getKey(), link);
			}
		}

		resources = new IdMap<>(RailResource.class, resourceMapping.size());
		for (Map.Entry<Id<RailResource>, List<RailLink>> e : resourceMapping.entrySet()) {

			// use type of the first link
			RailLink link = e.getValue().get(0);
			ResourceType type = RailsimUtils.getResourceType(network.getLinks().get(link.getLinkId()));

			RailResourceInternal r = switch (type) {
				case fixedBlock -> new FixedBlockResource(e.getKey(), e.getValue());
				case movingBlock -> new MovingBlockResource(e.getKey(), e.getValue());
			};

			e.getValue().forEach(l -> l.setResource(r));

			resources.put(e.getKey(), r);
		}

		dla.initResources(resources);
	}

	/**
	 * All available resources.
	 */
	public Collection<RailResource> getResources() {
		return resources.values();
	}

	/**
	 * Get single link that belongs to an id.
	 */
	public RailLink getLink(Id<Link> id) {
		return links.get(id);
	}


	/**
	 * Try to block a track and the underlying resource and return the allowed distance.
	 */
	public double tryBlockLink(double time, RailLink link, int track, TrainPosition position) {

		double reservedDist = link.resource.getReservedDist(link, position);

		// return only fully reserved links
		if (reservedDist != RailResourceInternal.NO_RESERVATION && reservedDist == link.length) {
			return reservedDist;
		}

		// For non-blocking areas a whole segment of links needs to be reserved
		if (link.isNonBlockingArea()) {

			List<RailLink> route = position.getRouteUntilNextStop();
			int idx = route.indexOf(link);

			List<RailLink> links = new LinkedList<>();
			boolean allFree = true;

			// After the non-blocking segment, reserve enough area for the train to hold if needed
			double avoidanceDist = 0;

			for (int i = idx; i < route.size(); i++) {
				RailLink l = route.get(i);

				// Note that the deadlock avoidance is not checked here on the single links
				// It will be invoked later on the whole segment of links
				allFree = l.resource.hasCapacity(time, l, track, position);
				if (!allFree)
					break;

				links.addFirst(l);

				// Accumulate the distance after the non-blocking area
				if (!l.isNonBlockingArea())
					avoidanceDist += l.length;

				if (avoidanceDist > position.getTrain().length())
					break;
			}

			if (!allFree || !dla.checkLinks(time, links, position)) {
				return RailResourceInternal.NO_RESERVATION;
			}

			double dist = RailResourceInternal.NO_RESERVATION;
			for (RailLink l : links) {
				dist = l.resource.reserve(time, l, track, position);
				eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), l.getLinkId(),
					position.getDriver().getVehicle().getId(), l.resource.getState(l)));
				dla.onReserve(time, l.resource, position);
			}

			return dist;
		}

		// Check and reserve a single link
		boolean blockedByRelatedTrain = isBlockedByRelatedTrain(link, position);
		if (link.resource.hasCapacity(time, link, track, position) || blockedByRelatedTrain) {

			if (!dla.checkLink(time, link, position)) {
				assert reservedDist == RailResourceInternal.NO_RESERVATION : "Link should not be reserved already.";

				return RailResourceInternal.NO_RESERVATION;
			}

			double dist = link.resource.reserve(time, link, track, position, blockedByRelatedTrain);
			eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(),
				position.getDriver().getVehicle().getId(), link.resource.getState(link)));

			dla.onReserve(time, link.resource, position);

			assert dist >= 0 : "Reserved distance must be equal or larger than 0.";

			return dist;
		}

		// may be previously reserved dist, or no reservation
		return reservedDist;
	}

	/**
	 * Vehicle ids with the same units are allowed together on a track, they will effectively become the same vehicle when merging.
	 */
	private boolean isBlockedByRelatedTrain(RailLink link, TrainPosition position) {
		TransitStopFacility stop = position.getNextStop();

		// Only transit stops
		if (stop == null || !Objects.equals(link.getLinkId(), stop.getLinkId()))
			return false;

		List<Id<Vehicle>> relatedVehicles = trains.getRelatedVehicles(position.getDriver().getVehicle().getId());

		for (Id<Vehicle> vehicle : relatedVehicles) {
			TrainPosition state = trains.getActiveTrain(vehicle);
			if (state != null && isBlockedBy(link, state)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether a link or underlying resource has remaining capacity.
	 */
	public boolean hasCapacity(double time, Id<Link> link, int track, TrainPosition position) {
		RailLink l = getLink(link);
		return l.resource.hasCapacity(time, l, track, position);
	}

	/**
	 * Set the capacity of a link or underlying resource.
	 */
	public void setCapacity(Id<Link> link, int newCapacity) {
		RailLink l = getLink(link);
		l.resource.setCapacity(newCapacity);
	}

	/**
	 * Whether a driver already reserved a link.
	 */
	public boolean isBlockedBy(RailLink link, TrainPosition position) {
		return link.resource.getReservedDist(link, position) > RailResourceInternal.NO_RESERVATION;
	}

	/**
	 * Release a non-free track to be free again.
	 */
	public void releaseLink(double time, RailLink link, MobsimDriverAgent driver) {

		boolean release = link.resource.release(link, driver);

		dla.onReleaseLink(time, link, driver);

		if (release) {
			dla.onRelease(time, link.resource, driver);
		}

		eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(), driver.getVehicle().getId(),
			link.resource.getState(link)));
	}

	/**
	 * Check if a re-route is allowed.
	 *
	 * @see DeadlockAvoidance#checkReroute(double, RailLink, RailLink, List, List, TrainPosition)
	 */
	public boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position) {
		return dla.checkReroute(time, start, end, subRoute, detour, position);
	}
}
