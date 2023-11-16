package org.matsim.core.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Some methods to validate DisallowedNextLinks attributes of a network.
 * 
 * @author hrewald
 */
public class DisallowedNextLinksUtils {

	private static final Logger LOG = LogManager.getLogger(DisallowedNextLinksUtils.class);

	private DisallowedNextLinksUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Check network for errors in the definition of disallowed next links and log
	 * them, if any.
	 * 
	 * @see {@link DisallowedNextLinks}
	 * 
	 * @param network
	 * @return true if no errors regarding disallowed next links were detected
	 */
	public static boolean isValid(Network network) {

		Map<Id<Link>, ? extends Link> links = network.getLinks();
		List<String> errors = links.entrySet().parallelStream()
				.map(e -> {
					DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(e.getValue());
					return disallowedNextLinks != null ? Map.entry(e.getKey(), disallowedNextLinks) : null;
				})
				.filter(e -> e != null)
				.flatMap(e -> getErrors(links, e.getKey(), e.getValue()).stream())
				.toList();

		errors.forEach(LOG::warn);
		return errors.isEmpty();
	}

	// Helpers

	private static List<String> getErrors(Map<Id<Link>, ? extends Link> links, Id<Link> linkId,
			DisallowedNextLinks disallowedNextLinks) {

		List<String> errors = new ArrayList<>();

		Link link = links.get(linkId);
		for (Entry<String, List<List<Id<Link>>>> entry : disallowedNextLinks.getAsMap().entrySet()) {
			List<List<Id<Link>>> linkSequences = entry.getValue();
			for (List<Id<Link>> linkSequence : linkSequences) {
				errors.addAll(isNextLinkSequenceOf(links, link, linkSequence));
			}
		}
		return errors;
	}

	private static List<String> isNextLinkSequenceOf(Map<Id<Link>, ? extends Link> links,
			Link link, List<Id<Link>> nextLinkIds) {

		List<String> messages = new ArrayList<>();

		Link lastLink = link;
		for (Id<Link> nextLinkId : nextLinkIds) {

			// all link ids in disallowedNextLinks need be subsequent links
			if (!isNextLinkOf(lastLink, nextLinkId)) {
				messages.add(String.format("Link %s had a next link sequence that is not valid sequence: %s",
						link.getId(), nextLinkIds));
			}
			lastLink = links.get(nextLinkId);

			// all link ids in disallowedNextLinks need to exist
			if (lastLink == null) {
				messages.add(String.format("Link %s had a next link sequence with (a) missing link(s): %s",
						link.getId(), nextLinkId));
			}
		}

		return messages;
	}

	private static boolean isNextLinkOf(Link link, Id<Link> nextLinkId) {
		Node toNode = link.getToNode();
		return toNode.getOutLinks().get(nextLinkId) != null;
	}

}
