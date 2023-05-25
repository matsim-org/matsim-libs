package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class responsible for managing and blocking resources and segments of links.
 */
public final class RailResourceManager {

	/**
	 * Rail links
	 */
	private final Map<Id<Link>, RailLink> links;


	private final Map<Id<RailResource>, RailResource> resources;

	/**
	 * Construct resources from network.
	 */
	public RailResourceManager(RailsimConfigGroup config, Network network) {
		this.links = new IdMap<>(Link.class, network.getLinks().size());

		Set<String> modes = config.getRailNetworkModes();
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
			resources.put(e.getKey(), new RailResource(e.getValue()));
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
		return resources.get(id);
	}

	/**
	 * Try to block a resource for a specific driver.
	 *
	 * @return true if the resource is now blocked or was blocked for this driver already.
	 */
	public boolean tryBlockResource(RailResource resource, MobsimDriverAgent driver) {

		if (resource.reservations.contains(driver))
			return true;

		if (resource.hasCapacity()) {
			resource.reservations.add(driver);
			return true;
		}

		return false;
	}

	/**
	 * Try to release a resource, but only if none of the links are blocked anymore by this driver.
	 *
	 * @return whether driver is still blocking this resource.
	 */
	public boolean tryReleaseResource(RailResource resource, MobsimDriverAgent driver) {

		if (resource.links.stream().noneMatch(l -> l.isBlockedBy(driver))) {
			resource.reservations.remove(driver);
			return true;
		}

		return false;
	}
}
