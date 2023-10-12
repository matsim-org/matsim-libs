package org.matsim.core.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

	private final Map<String, List<List<Id<Link>>>> linkIdSequencesMap = new HashMap<>();

	public boolean addDisallowedLinkSequence(String mode, Collection<Id<Link>> linkSequence) {
		List<List<Id<Link>>> sequences = this.linkIdSequencesMap.computeIfAbsent(mode, m -> new ArrayList<>());
		// prevent adding empty/duplicate link id sequences, or duplicate link ids
		if (!linkSequence.isEmpty() && new HashSet<>(linkSequence).size() == linkSequence.size()
				&& !sequences.contains(linkSequence)) {
			return sequences.add(ImmutableList.copyOf(linkSequence));
		}
		return false;
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DisallowedNextLinks dls) {
			return this.linkIdSequencesMap.equals(dls.linkIdSequencesMap);
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
