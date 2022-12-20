package org.matsim.core.population.routes.mediumcompressed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Implementation of {@link NetworkRoute} that stores the links of the route
 * using variable-length integer encoding (VarInt) instead of a List of link-ids.</p>
 *
 * <p>Each link-id of type Id uses 4 or 8 bytes, whereas the VarInt encoding
 * will likely only take at most 3 (for small to medium-sized scenarios)
 * or 4 bytes (for very large scenarios). So especially for large scenarios, where
 * object pointers use 8 bytes, this saves at least half the memory.</p>
 *
 * <p>There is a small performance overhead, but it is rather small compared
 * to {@link org.matsim.core.population.routes.heavycompressed.HeavyCompressedNetworkRoute}.
 * </p>
 *
 * @author mrieser / Simunto
 */
public class MediumCompressedNetworkRoute extends AbstractNetworkRoute {

	private int[] route = null;

	public MediumCompressedNetworkRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		this.setStartLinkId(startLinkId);
		this.setEndLinkId(endLinkId);
	}

	@Override
	public void setLinkIds(Id<Link> startLinkId, List<Id<Link>> linkIds, Id<Link> endLinkId) {
		this.setStartLinkId(startLinkId);
		this.setEndLinkId(endLinkId);
		int linkCount = linkIds == null ? 0 : linkIds.size();
		int[] route = new int[linkCount];
		int i = 0;
		if (linkIds != null) {
			for (Id<Link> linkId : linkIds) {
				route[i] = linkId.index();
				i++;
			}
		}
		this.route = route;
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		if (this.route == null) {
			return Collections.emptyList();
		}
		List<Id<Link>> linkIds = new ArrayList<>(this.route.length);
		for (int linkIndex : this.route) {
			linkIds.add(Id.get(linkIndex, Link.class));
		}
		return linkIds;
	}

	@Override
	public MediumCompressedNetworkRoute clone() {
		return (MediumCompressedNetworkRoute) super.clone();
	}

}
