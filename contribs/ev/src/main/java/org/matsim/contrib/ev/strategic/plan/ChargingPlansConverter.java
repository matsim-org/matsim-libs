package org.matsim.contrib.ev.strategic.plan;

import java.io.IOException;

import org.matsim.utils.objectattributes.AttributeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to serialize and deserialize the charging plans of an
 * agent which are saved as an attribute of regular plans.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlansConverter implements AttributeConverter<ChargingPlans> {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public ChargingPlans convert(String value) {
		try {
			return objectMapper.readValue(value, ChargingPlans.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String convertToString(Object o) {
		try {
			return objectMapper.writeValueAsString((ChargingPlans) o);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
