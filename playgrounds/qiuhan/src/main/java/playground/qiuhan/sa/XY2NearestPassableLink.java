/**
 * 
 */
package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Assigns a coordinate a referenced car-link that has a positive capacity and
 * freespeed *
 * 
 * @author Q. SUN
 * 
 */
public class XY2NearestPassableLink {
	private final static Logger log = Logger
			.getLogger(XY2NearestPassableLink.class.getName());
	private static Map<Coord, Link> foundLinks = new HashMap<Coord, Link>();

	public static Link getNearestPassableLink(Coord coord, NetworkImpl network) {
		Set<String> allowedModes = new TreeSet<String>();
		allowedModes.add(TransportMode.car);
		return getNearestPassableLink(coord, network, allowedModes);
	}

	public static Link getNearestPassableLink(Coord coord, NetworkImpl network,
			Set<String> allowedModes) {
		Link result = foundLinks.get(coord);
		if (result != null) {
			return result;
		}

		result = getNearestPassableLinkFromNearNodes(coord, network,
				allowedModes);
		foundLinks.put(coord, result);
		return result;
	}

	private static boolean isPassable(Link link, Set<String> allowedModes) {
		Set<String> linkAllowedModes = link.getAllowedModes();
		return linkAllowedModes != null
				&& linkAllowedModes.containsAll(allowedModes)
				&& link.getCapacity() > 0d && link.getFreespeed() > 0d;
	}

	private static Link getNearestPassableLinkFromOneNode(Coord coord,
			NetworkImpl network, Set<String> allowedModes, Node node) {
		Link nearestLink = null;

		double shortestDistance = Double.MAX_VALUE;
		for (Link link : NetworkUtils.getIncidentLinks(node).values()) {
			if (isPassable(link, allowedModes)) {// filter link
				double dist = ((LinkImpl) link).calcDistance(coord);
				if (dist < shortestDistance) {
					shortestDistance = dist;
					nearestLink = link;
				}
			}
		}

		return nearestLink;
	}

	private static Link getNearestPassableLinkFromNearNodes(Coord coord,
			NetworkImpl network, Set<String> allowedModes) {
		Link nearestLink = null;
		double distance = 500d;
		int cnt = 0;
		while (nearestLink == null && cnt < 10) {
			TreeMap<Double, Node> nodes = new TreeMap<Double, Node>();
			for (Node nearNode : network.getNearestNodes(coord, distance)) {
				if (nearNode != null
						&& !(nearNode.getInLinks().isEmpty() && nearNode
								.getOutLinks().isEmpty())) {
					nodes.put(
							CoordUtils.calcDistance(coord, nearNode.getCoord()),
							nearNode);
				}
			}
			for (Node nearNode : nodes.values()) {
				nearestLink = getNearestPassableLinkFromOneNode(coord, network,
						allowedModes, nearNode);
				if (nearestLink != null) {
					return nearestLink;
				}
			}
			cnt++;
			log.info("Count:\t" + cnt);
			distance *= 2d;
		}
		if (nearestLink == null) {
			log.warning("[nearestLink not found.  Will probably crash eventually ...  Maybe run NetworkCleaner?]");
		}
		return nearestLink;
	}
}
