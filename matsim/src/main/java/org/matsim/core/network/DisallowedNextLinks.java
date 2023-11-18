package org.matsim.core.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.google.common.collect.ImmutableList;

/**
 * Class to store disallowed next links, e.g. to model turn restrictions.
 * 
 * @author hrewald, mrieser
 */
public class DisallowedNextLinks {

	private static final Logger LOG = LogManager.getLogger(DisallowedNextLinks.class);

	private static boolean warnedAboutNotConsideredInRouting = false; // ! remove, if routing considers this

	// Actually, it could be Map<String, Set<List<Id<Link>>>>, as the order of the
	// next links sequences does not matter. However, we choose to store them in a
	// list in favor of a smaller memory footprint.
	private final Map<String, List<List<Id<Link>>>> linkIdSequencesMap = new HashMap<>();

	public DisallowedNextLinks() { // ! remove constructor, if routing considers this
		if (!warnedAboutNotConsideredInRouting) {
			warnedAboutNotConsideredInRouting = true;
			LOG.warn("Considering DisallowedNextLinks in routing is not yet implemented!");
		}
	}

	/**
	 * Add a sequence of subsequent links to be disallowed from the current link.
	 * 
	 * @param mode
	 * @param linkSequence sequence of links that shall not be passed after passing
	 *                     the link where this object is attached
	 * @return true, if linkSequence was actually added
	 */
	public boolean addDisallowedLinkSequence(String mode, List<Id<Link>> linkSequence) {
		List<List<Id<Link>>> linkSequences = this.linkIdSequencesMap.computeIfAbsent(mode, m -> new ArrayList<>());

		// prevent adding empty/duplicate link id sequences, or duplicate link ids
		if (linkSequence.isEmpty() || new HashSet<>(linkSequence).size() != linkSequence.size()
				|| linkSequences.contains(linkSequence)) {
			return false;
		}

		if (!linkSequences.add(ImmutableList.copyOf(linkSequence))) {
			return false;
		}

		// Semantically, the order does not matter. But despite using a list, we want
		// DisallowedNextLinks objects to have a working equal method. To ensure, that
		// DisallowedNextLinks are equal, even if links are added in a different order,
		// we sort the internal list.
		Collections.sort(linkSequences, (l, r) -> l.toString().compareTo(r.toString()));
		return true;
	}

	public List<List<Id<Link>>> getDisallowedLinkSequences(String mode) {
		List<List<Id<Link>>> sequences = this.linkIdSequencesMap.get(mode);
		return sequences != null ? Collections.unmodifiableList(sequences) : Collections.emptyList();
	}

	@Nullable
	public List<List<Id<Link>>> removeDisallowedLinkSequences(String mode) {
		return this.linkIdSequencesMap.remove(mode);
	}

	public Map<String, List<List<Id<Link>>>> getAsMap() {
		return this.linkIdSequencesMap.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, e -> Collections.unmodifiableList(e.getValue())));
	}

	public void clear() {
		this.linkIdSequencesMap.clear();
	}

	public boolean isEmpty() {
		return this.linkIdSequencesMap.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DisallowedNextLinks dnl
				// both linkIdSequences have same modes
				&& this.linkIdSequencesMap.keySet().equals(dnl.linkIdSequencesMap.keySet())) {
			for (Entry<String, List<List<Id<Link>>>> entry : this.linkIdSequencesMap.entrySet()) {
				String mode = entry.getKey();
				List<List<Id<Link>>> linkSequences = entry.getValue();
				List<List<Id<Link>>> otherLinkSequences = dnl.linkIdSequencesMap.get(mode);
				// because we store next link sequences in a list, even though their order has
				// no meaning, we need to ignore the order when comparing objects.
				if (linkSequences.size() != otherLinkSequences.size()
						|| !linkSequences.containsAll(otherLinkSequences)
						|| !otherLinkSequences.containsAll(linkSequences)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.linkIdSequencesMap.hashCode();
	}

}
