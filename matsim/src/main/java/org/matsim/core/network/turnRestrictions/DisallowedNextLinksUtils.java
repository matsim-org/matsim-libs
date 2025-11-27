package org.matsim.core.network.turnRestrictions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;

/**
 * Some methods to validate DisallowedNextLinks attributes of a network.
 * 
 * @author hrewald
 */
public final class DisallowedNextLinksUtils {

	private static final Logger LOG = LogManager.getLogger(DisallowedNextLinksUtils.class);

	private static final String I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE = "iAmANextLinkOfTheseFromLinks";

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

		Set<Id<Link>> loopLinkIds = links.values().stream()
				.filter(link -> link.getFromNode().getId().equals(link.getToNode().getId()))
				.map(Link::getId)
				.collect(Collectors.toSet());

		links.values().forEach(link -> {

			DisallowedNextLinks dnl = NetworkUtils.getDisallowedNextLinks(link);
			if (dnl == null) {
				return;
			}

			// remove dnls on loop links
			if (loopLinkIds.contains(link.getId())) {
				NetworkUtils.removeDisallowedNextLinks(link);
				LOG.info("Link {}: removed disallowed next link sequences from loop link", link.getId());
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
						// remove loop links from sequences
						.map(linkIds -> linkIds.stream()
								.filter(linkId -> !loopLinkIds.contains(linkId))
								.toList())
						.filter(linkIds -> !linkIds.isEmpty())
						.toList();

				// update mode with valid link sequences
				dnl.removeDisallowedLinkSequences(mode);
				validLinkSequences.forEach(linkIds -> dnl.addDisallowedLinkSequence(mode, linkIds));
				int invalidLinkSequencesCount = linkSequences.size() - validLinkSequences.size();
				if (invalidLinkSequencesCount > 0) {
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

	/**
	 * Copy link sequences of DisallowedNextLinks attributes from one mode to
	 * another e.g., from car to ride.
	 * 
	 * @param network
	 * @param fromMode
	 * @param toMode
	 * @see {@link #clean(Network)}
	 */
	public static void copy(Network network, String fromMode, String toMode) {
		network.getLinks().values().forEach(link -> {

			DisallowedNextLinks dnl = NetworkUtils.getDisallowedNextLinks(link);
			if (dnl == null) {
				return;
			}

			for (List<Id<Link>> linkSequence : dnl.getDisallowedLinkSequences(fromMode)) {
				dnl.addDisallowedLinkSequence(toMode, linkSequence);
			}

		});
	}

	/**
	 * Returns a list of link id sequences that are not allowed to be traveled due
	 * to turn restrictions.
	 * 
	 * @param network
	 * @param mode    use turn restrictions of that mode
	 * @return
	 */
	public static List<List<Id<Link>>> getDisallowedLinkIdSequences(Network network, String mode) {
		return network.getLinks().values().stream()
				.map(link -> {
					DisallowedNextLinks dnl = NetworkUtils.getDisallowedNextLinks(link);
					List<List<Id<Link>>> disallowedLinkSequences = Collections.emptyList();
					if (dnl != null) {
						disallowedLinkSequences = dnl.getDisallowedLinkSequences(mode);
					}
					return Map.entry(link.getId(), disallowedLinkSequences);
				})
				.filter(e -> !e.getValue().isEmpty())
				.map(e -> {
					List<List<Id<Link>>> linkSequences = new ArrayList<>(e.getValue().size());
					for (List<Id<Link>> disallowedNextLinks : e.getValue()) {
						List<Id<Link>> linkIds = new ArrayList<>(disallowedNextLinks.size() + 1);
						linkIds.add(e.getKey()); // add this link at start of link id sequence
						linkIds.addAll(disallowedNextLinks);
						linkSequences.add(linkIds);
					}
					return linkSequences;
				})
				.flatMap(List::stream)
				.toList();
	}

	/**
	 * Write from link of disallowed link object to its next links. This is required
	 * for being able to simplify a network with {@link DisallowedNextLinks} objects
	 * correctly.
	 * 
	 * @param network
	 * @see #removeAnnotation(Network)
	 */
	public static void annotateNetworkForSimplification(Network network) {

		LOG.info("Call removeAnnotation() to remove temporarily added attributes after simplification.");

		for (Link link : network.getLinks().values()) {
			DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(link);
			if (disallowedNextLinks != null) {
				disallowedNextLinks.getAsMap().values().stream()
						.flatMap(List::stream)
						.flatMap(List::stream)
						.map(network.getLinks()::get)
						.forEach(l -> {
							Set<Id<Link>> fromLinks = (Set<Id<Link>>) l.getAttributes()
									.getAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);
							if (fromLinks == null) {
								fromLinks = new HashSet<>();
							}
							fromLinks.add(link.getId());
							l.getAttributes().putAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE, fromLinks);
						});
			}
		}
	}

	/**
	 * Removes annotation from {@link #annotateNetworkForSimplification(Network)}.
	 * 
	 * @param network
	 */
	public static void removeAnnotation(Network network) {
		for (Link link : network.getLinks().values()) {
			link.getAttributes().removeAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);
		}
	}

	/**
	 * Create a predicate that prevents merging links that would destroy turn
	 * restriction info of {@link DisallowedNextLinks} attributes.
	 * 
	 * @param network
	 * @return
	 * @see #createTransferAttributesConsumer(Network)
	 */
	public static BiPredicate<Link, Link> createIsMergeablePredicate(Network network) {
		return (link1, link2) -> {

			DisallowedNextLinks dnl1 = NetworkUtils.getDisallowedNextLinks(link1);
			DisallowedNextLinks dnl2 = NetworkUtils.getDisallowedNextLinks(link2);
			@SuppressWarnings("unchecked")
			Set<Id<Link>> fromLinkIds1 = (Set<Id<Link>>) link1.getAttributes()
					.getAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);
			@SuppressWarnings("unchecked")
			Set<Id<Link>> fromLinkIds2 = (Set<Id<Link>>) link2.getAttributes()
					.getAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);

			// links are themselves from links of dnl
			// -> do not merge
			if (dnl1 != null || dnl2 != null) {
				return false;
			}

			// no disallowed next link involvement at all
			// -> safe merge
			if (fromLinkIds1 == null && fromLinkIds2 == null) {
				return true;
			}

			// both are subsequent links in all dnls they are part of, and are not from
			// links
			// -> can merge, but need to adjust DNL objects

			// get set of from links where these links are in those dnls
			Set<Id<Link>> commonFromLinkIds = new HashSet<>();
			if (fromLinkIds1 != null) {
				commonFromLinkIds.addAll(fromLinkIds1);
			}
			if (fromLinkIds2 != null) {
				commonFromLinkIds.addAll(fromLinkIds2);
			}
			List<DisallowedNextLinks> dnls = commonFromLinkIds.stream()
					.map(network.getLinks()::get)
					.map(NetworkUtils::getDisallowedNextLinks)
					.filter(Objects::nonNull)
					.toList();

			// check, that both links are subsequent in all dnls
			for (DisallowedNextLinks dnl : dnls) {
				Map<String, List<List<Id<Link>>>> dnlMap = dnl.getAsMap();
				for (Entry<String, List<List<Id<Link>>>> entry : dnlMap.entrySet()) {
					for (List<Id<Link>> linkIdList : entry.getValue()) {
						int firstLinkIdx = -1;
						int secondLinkIdx = -1;
						for (ListIterator<Id<Link>> it = linkIdList.listIterator(); it.hasNext();) {
							int idx = it.nextIndex();
							Id<Link> linkId = it.next();
							if (firstLinkIdx < 0 && linkId.equals(link1.getId())) {
								firstLinkIdx = idx;
							} else if (firstLinkIdx >= 0 && secondLinkIdx < 0 && linkId.equals(link2.getId())) {
								secondLinkIdx = idx;
							} else {
								break; // both found
							}
						}

						if (!(firstLinkIdx == linkIdList.size() - 1 && secondLinkIdx < 0) // first link is not last
																							// link
								&& !(firstLinkIdx >= 0 && secondLinkIdx == firstLinkIdx + 1) // links are not
																								// subsequent
						) {
							return false;
						}
					}
				}
			}
			return true;
		};
	}

	/**
	 * Create a BiConsumer that merges two links, considering turn restrictions.
	 * 
	 * @param network
	 * @return
	 * @see #createIsMergeablePredicate(Network)
	 */
	public static BiConsumer<Tuple<Link, Link>, Link> createTransferAttributesConsumer(Network network) {
		return (t, newLink) -> {

			// isMergeableWithDisallowedLinks does not allow merging of from links of
			// DisallowedNextLinks, so we only need to consider these cases:
			// a) the first link is the last link of a next links-list
			// b) both links are subsequent in the list of next links
			// In both cases, we need to update the list of next links of the respective DNL
			// object with the new link id

			Link link1 = t.getFirst();
			Link link2 = t.getSecond();

			@SuppressWarnings("unchecked")
			Set<Id<Link>> fromLinkIds1 = (Set<Id<Link>>) link1.getAttributes()
					.getAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);
			@SuppressWarnings("unchecked")
			Set<Id<Link>> fromLinkIds2 = (Set<Id<Link>>) link2.getAttributes()
					.getAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE);
			// get set of from links where these links are in those dnls
			Map<Link, DisallowedNextLinks> linkDnl = getLinkDnlMap(network, fromLinkIds1, fromLinkIds2);

			// change disallowed next links
			for (Entry<Link, DisallowedNextLinks> linkDnlEntry : linkDnl.entrySet()) {
				final Link fromLink = linkDnlEntry.getKey();
				Map<String, List<List<Id<Link>>>> dnlMap = linkDnlEntry.getValue().getAsMap();

				// create new DisallowedNextLink object
				DisallowedNextLinks newDnl = new DisallowedNextLinks();
				for (Entry<String, List<List<Id<Link>>>> entry : dnlMap.entrySet()) {
					final String mode = entry.getKey();
					for (List<Id<Link>> linkIdList : entry.getValue()) {
						List<Id<Link>> newLInkIdList = new ArrayList<>();
						for (Id<Link> linkId : linkIdList) {
							if (linkId.equals(link1.getId())) {
								// add new link instead of (old) first link
								newLInkIdList.add(newLink.getId());
							} else if (linkId.equals(link2.getId())) {
								// do not add (old) second link
							} else {
								newLInkIdList.add(linkId);
							}
						}
						newDnl.addDisallowedLinkSequence(mode, newLInkIdList);
					}
				}
				NetworkUtils.setDisallowedNextLinks(fromLink, newDnl);
			}
			// add attribute to new link, that remembers for which from links this new link
			// is in the next
			// links-list
			if (!linkDnl.isEmpty()) {
				newLink.getAttributes().putAttribute(I_AM_A_NEXT_LINK_OF_THESE_FROM_LINKS_ATTRIBUTE,
						linkDnl.keySet().stream()
								.map(Link::getId)
								.collect(Collectors.toSet()));
			}

		};
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

	private static Map<Link, DisallowedNextLinks> getLinkDnlMap(Network network, Set<Id<Link>> fromLinkIds1,
			Set<Id<Link>> fromLinkIds2) {
		Set<Id<Link>> commonFromLinkIds = new HashSet<>();
		if (fromLinkIds1 != null) {
			commonFromLinkIds.addAll(fromLinkIds1);
		}
		if (fromLinkIds2 != null) {
			commonFromLinkIds.addAll(fromLinkIds2);
		}
		return commonFromLinkIds.stream()
				.map(network.getLinks()::get)
				.map(l -> Map.entry(l, NetworkUtils.getDisallowedNextLinks(l)))
				.filter(e -> e.getValue() != null)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

}
