package org.matsim.utils.objectattributes.attributeconverters;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdDeSerializationModule;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.DisallowedNextLinks;
import org.matsim.utils.objectattributes.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Class to store {@link DisallowedNextLinks}.
 * 
 * @author hrewald
 */
public class DisallowedNextLinksAttributeConverter implements AttributeConverter<DisallowedNextLinks> {

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

		DisallowedNextLinks dnl = new DisallowedNextLinks();
		for (Entry<String, List<List<Id<Link>>>> entry : linkIdSequencesMap.entrySet()) {
			String mode = entry.getKey();
			for (List<Id<Link>> linkIdList : entry.getValue()) {
				dnl.addDisallowedLinkSequence(mode, linkIdList);
			}
		}
		return dnl;
	}

	@Override
	public String convertToString(Object o) {
		if (o instanceof DisallowedNextLinks dnl) {
			try {
				return OBJECT_WRITER.writeValueAsString(dnl.getAsMap());
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		throw new IllegalArgumentException();
	}

}