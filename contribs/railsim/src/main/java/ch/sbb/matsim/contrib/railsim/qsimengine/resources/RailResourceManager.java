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
	public RailResourceManager(QSim qsim, DeadlockAvoidance dla) {
		this(qsim.getEventsManager(),
			ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), RailsimConfigGroup.class),
			qsim.getScenario().getNetwork(),
			dla);
	}

	/**
	 * Construct resources from network.
	 */
	public RailResourceManager(EventsManager eventsManager, RailsimConfigGroup config,
							   Network network, DeadlockAvoidance dla) {
		this.eventsManager = eventsManager;
		this.dla = dla;
		this.links = new IdMap<>(Link.class, network.getLinks().size());

		// Mapping for resources to be created
		Map<Id<RailResource>, List<RailLink>> resourceMapping = new HashMap<>();

		Set<String> modes = config.getNetworkModes();
		for (Map.Entry<Id<Link>, ? extends Link> e : network.getLinks().entrySet()) {
			if (e.getValue().getAllowedModes().stream().anyMatch(modes::contains)) {

				RailLink link = new RailLink(e.getValue());
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

		if (link.resource.hasCapacity(time, link, track, position)) {

			if (!dla.checkLink(time, link, position)) {
				assert reservedDist == RailResourceInternal.NO_RESERVATION : "Link should not be reserved already.";

				return RailResourceInternal.NO_RESERVATION;
			}

			double dist = link.resource.reserve(time, link, track, position);
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
	 * Checks whether a link or underlying resource has remaining capacity.
	 */
	public boolean hasCapacity(double time, Id<Link> link, int track, TrainPosition position) {
		RailLink l = getLink(link);
		return l.resource.hasCapacity(time, l, track, position);
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
	 * @see DeadlockAvoidance#checkReroute(double, RailLink, RailLink, List, List, TrainPosition)
	 */
	public boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position) {
		return dla.checkReroute(time, start, end, subRoute, detour, position);
	}
}
