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
	 * Retrieve source id of a link.
	 */
	public static Id<RailResource> getResourceId(Link link) {
		String id = RailsimUtils.getResourceId(link);
		if (id == null)
			return Id.create(link.getId().toString(), RailResource.class);
		else
			return Id.create(id, RailResource.class);
	}

	/**
	 * All available resources.
	 */
	public Collection<RailResource> getResources() {
		return resources.values();
	}

	/**
	 * Construct resources from network.
	 */
	public RailResourceManager(EventsManager eventsManager, RailsimConfigGroup config, Network network) {
		this.eventsManager = eventsManager;
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
	public double tryBlockLink(double time, TrainPosition position, RailLink link) {

		double reservedDist = link.resource.getReservedDist(link, position);
		if (reservedDist != RailResourceInternal.NO_RESERVATION) {
			return reservedDist;
		}

		if (link.resource.hasCapacity(link, position)) {

			double dist = link.resource.reserve(link, position);
			eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(),
				position.getDriver().getVehicle().getId(), link.resource.getState(link)));

			return dist;
		}

		return RailResourceInternal.NO_RESERVATION;
	}

	/**
	 * Checks whether a link or underlying resource has remaining capacity.
	 */
	public boolean hasCapacity(Id<Link> link, TrainPosition position) {
		RailLink l = getLink(link);
		return l.resource.hasCapacity(l, position);
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

		link.resource.release(link, driver);

		eventsManager.processEvent(new RailsimLinkStateChangeEvent(Math.ceil(time), link.getLinkId(), driver.getVehicle().getId(),
			link.resource.getState(link)));
	}
}
