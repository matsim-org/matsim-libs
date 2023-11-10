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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.FixedBlockResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.MovingBlockResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.QSim;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class responsible for managing and blocking resources and segments of links.
 */
public final class RailResourceManager {

	private final EventsManager eventsManager;

	/**
	 * Rail links.
	 */
	private final Map<Id<Link>, RailLink> links;

	private final Map<Id<RailResource>, RailResource> resources;

	@Inject
	public RailResourceManager(QSim qsim) {
		this(qsim.getEventsManager(), ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), RailsimConfigGroup.class), qsim.getScenario().getNetwork());
	}

	/**
	 * Construct resources from network.
	 */
	public RailResourceManager(EventsManager eventsManager, RailsimConfigGroup config, Network network) {
		this.eventsManager = eventsManager;
		this.links = new IdMap<>(Link.class, network.getLinks().size());

		Set<String> modes = config.getNetworkModes();
		for (Map.Entry<Id<Link>, ? extends Link> e : network.getLinks().entrySet()) {
			if (e.getValue().getAllowedModes().stream().anyMatch(modes::contains))
				this.links.put(e.getKey(), new RailLink(e.getValue()));
		}

		Map<Id<RailResource>, List<RailLink>> collect = links.values().stream()
			.filter(l -> l.resource != null)
			.collect(Collectors.groupingBy(l -> l.resource, Collectors.toList())
			);

		resources = new IdMap<>(RailResource.class, collect.size());
		for (Map.Entry<Id<RailResource>, List<RailLink>> e : collect.entrySet()) {
			if (e.getValue().stream().allMatch(l -> l.resourceType == ResourceType.movingBlock))
				resources.put(e.getKey(), new MovingBlockResource(e.getValue()));
			else if (e.getValue().stream().allMatch(l -> l.resourceType == ResourceType.movingBlock))
				throw new IllegalStateException("Cannot mix moving and fixed block resources.");
			else
				resources.put(e.getKey(), new FixedBlockResource(e.getValue()));
		}
	}

	/**
	 * Get single link that belongs to an id.
	 */
	public RailLink getLink(Id<Link> id) {
		return links.get(id);
	}

	/**
	 * Return the resource for a given id.
	 */
	public RailResource getResource(Id<RailResource> id) {
		if (id == null) return null;
		return resources.get(id);
	}

	/**
	 * Try to block a resource for a specific driver.
	 *
	 * @return true if the resource is now blocked or was blocked for this driver already.
	 */
	private boolean tryBlockResource(RailResource resource, TrainPosition position) {

		if (resource.isReservedBy(position.getDriver()))
			return true;

		// FIXME: pass actual time
		if (resource.hasCapacity(0, position)) {
			resource.reserve(position);
			return true;
		}

		return false;
	}

	/**
	 * Try to release a resource, but only if none of the links are blocked anymore by this driver.
	 *
	 * @return whether driver is still blocking this resource.
	 */
	private boolean tryReleaseResource(RailResource resource, MobsimDriverAgent driver) {

		if (resource.getLinks().stream().noneMatch(l -> l.isBlockedBy(driver))) {
			resource.release(driver);
			return true;
		}

		return false;
	}

	/**
	 * Try to block a track and the underlying resource and return whether it was successful.
	 */
	public boolean tryBlockTrack(double time, TrainPosition position, RailLink link) {
		if (link.isMovingBlock())
			return tryBlockTrackMoving(time, position, link);

		return tryBlockTrackFixed(time, position, link);
	}

	private boolean tryBlockTrackFixed(double time, TrainPosition position, RailLink link) {
		if (link.isBlockedBy(position.getDriver()))
			return true;

		Id<RailResource> resourceId = link.getResourceId();
		if (resourceId != null) {

			RailResource resource = getResource(resourceId);

			// resource is required
			if (!tryBlockResource(resource, position)) {
				return false;
			}
		}

		if (link.hasFreeTrack()) {
			int track = link.blockTrack(position.getDriver());
			eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(),
				position.getDriver().getVehicle().getId(), TrackState.BLOCKED, track));

			return true;
		}

		return false;
	}

	private boolean tryBlockTrackMoving(double time, TrainPosition position, RailLink link) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	/**
	 * Checks whether a link or underlying resource has remaining capacity.
	 */
	public boolean hasCapacity(Id<Link> link, TrainPosition position) {

		RailLink l = getLink(link);

		if (!l.hasFreeTrack())
			return false;

		RailResource res = getResource(l.getResourceId());
		if (res != null) {
			// FIXME: pass actual driver and time
			return res.hasCapacity(0, position);
		}

		return true;
	}

	/**
	 * Whether a driver already reserved a link or would be able to reserve it.
	 */
	public boolean isBlockedBy(RailLink link, MobsimDriverAgent driver) {
		// If a link is blocked, the resource must be blocked as well
		return link.isBlockedBy(driver);
	}

	/**
	 * Release a non-free track to be free again.
	 */
	public void releaseTrack(double time, MobsimDriverAgent driver, RailLink link) {
		int track = link.releaseTrack(driver);
		eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(), driver.getVehicle().getId(),
			TrackState.FREE, track));

		// Release held resources
		if (link.getResourceId() != null) {
			tryReleaseResource(getResource(link.getResourceId()), driver);
		}

	}
}
