package org.matsim.core.network.turnRestrictions;

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
import org.matsim.core.network.NetworkUtils;

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

	/**
	 * Remove link sequences of DisallowedNextLinks which contain missing links or
	 * wrong modes.
	 * 
	 * @param network
	 */
	public static void clean(Network network) {
		Map<Id<Link>, ? extends Link> links = network.getLinks();

		links.values().forEach(link -> {

			DisallowedNextLinks dnl = NetworkUtils.getDisallowedNextLinks(link);
			if (dnl == null) {
				return;
			}

			// remove link sequences for modes, that are not allowed on this link
			for (Entry<String, List<List<Id<Link>>>> entry : dnl.getAsMap().entrySet()) {
				final String mode = entry.getKey();
				final int linkSequencesCount = entry.getValue().size();

				if (!link.getAllowedModes().contains(mode)) {
					dnl.removeDisallowedLinkSequences(mode);
					LOG.info("Link {}: Removed all {} disallowed next link sequences of mode {}"
							+ " because {} is not allowed", link.getId(), linkSequencesCount, mode, mode);
				}
			}

			// keep only valid link sequences
			for (Entry<String, List<List<Id<Link>>>> entry : dnl.getAsMap().entrySet()) {
				final String mode = entry.getKey();
				final List<List<Id<Link>>> linkSequences = entry.getValue();

				// find valid link sequences
				List<List<Id<Link>>> validLinkSequences = linkSequences.stream()
						// links of sequence exist in network
						.filter(linkIds -> linkIds.stream().allMatch(links::containsKey))
						// links all have mode in allowed modes
						.filter(linkIds -> linkIds.stream()
								.map(links::get)
								.map(Link::getAllowedModes)
								.allMatch(allowedModes -> allowedModes.contains(mode)))
						.toList();

				// update mode with valid link sequences
				final int invalidLinkSequencesCount = linkSequences.size() - validLinkSequences.size();
				if (invalidLinkSequencesCount > 0) {
					dnl.removeDisallowedLinkSequences(mode);
					validLinkSequences.forEach(linkIds -> dnl.addDisallowedLinkSequence(mode, linkIds));
					LOG.info("Link {}: Removed {} disallowed next link sequences for mode {}",
							link.getId(), invalidLinkSequencesCount, mode);
				}
			}

			// remove attribute completely, if it contains no link sequences anymore.
			if (dnl.isEmpty()) {
				NetworkUtils.removeDisallowedNextLinks(link);
			}

		});
	}

	// Helpers

	private static List<String> getErrors(Map<Id<Link>, ? extends Link> links, Id<Link> linkId,
			DisallowedNextLinks disallowedNextLinks) {

		List<String> errors = new ArrayList<>();

		Link link = links.get(linkId);
		for (Entry<String, List<List<Id<Link>>>> entry : disallowedNextLinks.getAsMap().entrySet()) {
			String mode = entry.getKey();
			List<List<Id<Link>>> linkSequences = entry.getValue();

			for (List<Id<Link>> linkSequence : linkSequences) {
				// check for (1) link sequences being a valid sequence and (2) links existing
				errors.addAll(isNextLinkSequenceOf(links, link, linkSequence));

				// check for allowedModes on this and next links
				errors.addAll(isInAllowedModes(links, mode, link, linkSequence));
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

	private static List<String> isInAllowedModes(Map<Id<Link>, ? extends Link> links, String mode, Link link,
			List<Id<Link>> nextLinkIds) {

		List<String> messages = new ArrayList<>();

		if (!link.getAllowedModes().contains(mode)) {
			messages.add(String.format("Link %s does not allow mode %s",
					link.getId(), mode));
		}

		for (Id<Link> nextLinkId : nextLinkIds) {
			Link nextLink = links.get(nextLinkId);
			if (nextLink != null && !nextLink.getAllowedModes().contains(mode)) {
				messages.add(String.format("Next link %s does not allow mode %s",
						nextLink.getId(), mode));
			}
		}

		return messages;
	}

}
