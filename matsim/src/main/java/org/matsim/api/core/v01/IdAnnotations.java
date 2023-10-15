/* *********************************************************************** *
 * project: org.matsim.*
 * IdAnnotations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.api.core.v01;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public interface IdAnnotations {

	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonSerialize(using = JsonIdSerializer.class)
	@JsonDeserialize(using = JsonIdContextualDeserializer.class)
	public @interface JsonId {

	}

	class JsonIdSerializer<T> extends StdSerializer<T> {

		protected JsonIdSerializer() {
			this(null);
		}

		protected JsonIdSerializer(Class<T> vc) {
			super(vc);
		}

		@Override
		public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(value.toString());
		}

	}

	class JsonIdContextualDeserializer<T> extends StdDeserializer<Id<T>> implements ContextualDeserializer {

		private Class<T> idClass;

		protected JsonIdContextualDeserializer() {
			this(null);
		}

		protected JsonIdContextualDeserializer(Class<T> idClass) {
			super(Object.class);
			this.idClass = idClass;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
				throws JsonMappingException {

			final Class<? extends Object> idClass;
			{
				final JavaType type;
				if (property != null)
					type = property.getType();
				else {
					type = ctxt.getContextualType();
				}
				idClass = type.containedType(0).getRawClass();
			}

			return JsonIdDeserializer.getInstance(idClass);
		}

		@Override
		public Id<T> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
			JsonNode node = jp.getCodec().readTree(jp);
			return Id.create(node.asText(), idClass);
		}

	}

	class JsonIdDeserializer<T> extends StdDeserializer<Id<T>> {

		private static final Map<Class<?>, JsonIdDeserializer<?>> CACHE = new HashMap<>();

		public static JsonIdDeserializer<?> getInstance(Class<?> clazz) {
			return CACHE.computeIfAbsent(clazz, k -> new JsonIdDeserializer<>(k));
		}

		private final Class<T> idClass;

		private JsonIdDeserializer() {
			this(null);
		}

		private JsonIdDeserializer(Class<T> idClass) {
			super(Object.class);
			this.idClass = idClass;
		}

		@Override
		public Id<T> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
			JsonNode node = jp.getCodec().readTree(jp);
			return Id.create(node.asText(), idClass);
		}

	}

}
