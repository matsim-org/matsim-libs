package org.matsim.core.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdDeSerializationModule;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;

/**
 * Class to store disallowed next links, e.g. to model turn restrictions.
 * 
 * @author hrewald, mrieser
 */
public class DisallowedNextLinks {

	// Actually, it could be Map<String, Set<List<Id<Link>>>>, as the order of the
	// next links sequences does not matter. However, we choose to store them in a
	// list in favor of a smaller memory footprint.
	private final Map<String, List<List<Id<Link>>>> linkIdSequencesMap = new HashMap<>();

	/**
	 * Add a sequence of subsequent links to be disallowed.
	 * 
	 * @param mode
	 * @param linkSequence sequence of links that shall not be passed after passing
	 *                     the link where this object is attached
	 * @return true, if linkSequence was actually added
	 */
	public boolean addDisallowedLinkSequence(String mode, Collection<Id<Link>> linkSequence) {
		List<List<Id<Link>>> linkSequences = this.linkIdSequencesMap.computeIfAbsent(mode, m -> new ArrayList<>());

		boolean result = false;
		// prevent adding empty/duplicate link id sequences, or duplicate link ids
		if (!linkSequence.isEmpty() && new HashSet<>(linkSequence).size() == linkSequence.size()
				&& !linkSequences.contains(linkSequence)) {
			result = linkSequences.add(ImmutableList.copyOf(linkSequence));
			if (result) { // sorting is required for a.equals(b) <=> a.hashCode() == b.hashCode()
				Collections.sort(linkSequences, (l, r) -> l.toString().compareTo(r.toString()));
			}
		}
		return result;
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
		if (obj instanceof DisallowedNextLinks dnl) {
			if (!this.linkIdSequencesMap.keySet().equals(dnl.linkIdSequencesMap.keySet())) {
				return false;
			}
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

	public static class DisallowedNextLinksAttributeConverter implements AttributeConverter<DisallowedNextLinks> {

		private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
		private static final ObjectWriter OBJECT_WRITER;
		private static final JavaType LINK_IDS_LIST_MAP_TYPE;
		static {
			// register Deserializers & Serializers for Ids
			OBJECT_MAPPER.registerModule(IdDeSerializationModule.getInstance());

			// build type & writer
			TypeFactory typeFactory = TypeFactory.defaultInstance();
			JavaType linkIdType = typeFactory.constructParametricType(Id.class, Link.class);
			CollectionType linkIdsType = typeFactory.constructCollectionType(List.class, linkIdType);
			CollectionType linkIdsListType = typeFactory.constructCollectionType(List.class, linkIdsType);
			LINK_IDS_LIST_MAP_TYPE = typeFactory.constructMapType(Map.class, typeFactory.constructType(String.class),
					linkIdsListType);
			OBJECT_WRITER = OBJECT_MAPPER.writerFor(LINK_IDS_LIST_MAP_TYPE);
		}

		@Override
		public DisallowedNextLinks convert(String value) {
			Map<String, List<List<Id<Link>>>> linkIdSequencesMap;
			try {
				linkIdSequencesMap = OBJECT_MAPPER.readValue(value, LINK_IDS_LIST_MAP_TYPE);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}

			DisallowedNextLinks dls = new DisallowedNextLinks();
			for (Entry<String, List<List<Id<Link>>>> entry : linkIdSequencesMap.entrySet()) {
				String mode = entry.getKey();
				for (List<Id<Link>> linkIdList : entry.getValue()) {
					dls.addDisallowedLinkSequence(mode, linkIdList);
				}
			}
			return dls;
		}

		@Override
		public String convertToString(Object o) {
			if (o instanceof DisallowedNextLinks dls) {
				try {
					return OBJECT_WRITER.writeValueAsString(dls.linkIdSequencesMap);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
			throw new IllegalArgumentException();
		}

	}

}
