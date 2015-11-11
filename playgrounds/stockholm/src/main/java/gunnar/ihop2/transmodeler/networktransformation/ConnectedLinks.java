package gunnar.ihop2.transmodeler.networktransformation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConnectedLinks {

	public static enum Dir {
		fwd, bwd
	};

	private ConnectedLinks() {
	}

	public static Set<Link> connectedLinks(final Link startLink,
			final Network network, final Dir dir,
			final Map<String, TransmodelerLink> id2tmLink) {

		final Set<Link> result = new LinkedHashSet<>();
		final Set<Link> activeLinks = new LinkedHashSet<>();
		activeLinks.add(startLink);

		while (!activeLinks.isEmpty()) {

			final Link expandedLink = activeLinks.iterator().next();
			activeLinks.remove(expandedLink);
			result.add(expandedLink);
			final TransmodelerLink expandedTmLink = id2tmLink.get(expandedLink
					.getId().toString());

			if (Dir.fwd.equals(dir)) {

				if (expandedTmLink.downstreamLink2turnLength != null) {
					for (Link downstreamLink : expandedLink.getToNode()
							.getOutLinks().values()) {
						if (!result.contains(downstreamLink)) {
							final TransmodelerLink downstreamTmLink = id2tmLink
									.get(downstreamLink.getId().toString());
							if (expandedTmLink.downstreamLink2turnLength
									.containsKey(downstreamTmLink)) {
								activeLinks.add(downstreamLink);
							}
						}
					}
				}

			} else {

				for (Link upstreamLink : expandedLink.getFromNode()
						.getInLinks().values()) {
					if (!result.contains(upstreamLink)) {
						final TransmodelerLink upstreamTmLink = id2tmLink
								.get(upstreamLink.getId().toString());
						if ((upstreamTmLink.downstreamLink2turnLength != null)
								&& upstreamTmLink.downstreamLink2turnLength
										.containsKey(expandedTmLink)) {
							activeLinks.add(upstreamLink);
						}
					}
				}
			}

		}

		return result;
	}

	public static Set<Link> connectedLinks(final Network network, 
			final Map<String, TransmodelerLink> id2tmLink) {
		final Link startLink = network.getLinks().values().iterator().next();
		final Set<Link> result = connectedLinks(startLink, network, Dir.fwd, id2tmLink);
		result.retainAll(connectedLinks(startLink, network, Dir.bwd, id2tmLink));
		return result;
	}
}
