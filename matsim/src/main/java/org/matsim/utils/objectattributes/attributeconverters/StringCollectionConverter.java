package org.matsim.utils.objectattributes.attributeconverters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Collection;
import java.util.Collections;

public class StringCollectionConverter implements AttributeConverter<Collection<String>> {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(Collection.class, String.class);

	@Override
	public Collection<String> convert(String value) {
		try {
			return Collections.unmodifiableCollection(mapper.readValue(value, collectionType));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String convertToString(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
