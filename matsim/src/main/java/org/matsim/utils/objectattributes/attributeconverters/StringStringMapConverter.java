package org.matsim.utils.objectattributes.attributeconverters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Collections;
import java.util.Map;

public class StringStringMapConverter implements AttributeConverter<Map<String, String>> {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final MapType mapType = TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class);

	@Override
	public Map<String, String> convert(String value) {
		try {
			return Collections.unmodifiableMap(mapper.readValue(value, mapType));
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
