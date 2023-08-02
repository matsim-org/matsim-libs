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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public interface IdAnnotations {

	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonSerialize(using = JsonPersonId.PersonIdSerializer.class)
	@JsonDeserialize(using = JsonPersonId.PersonIdDeserializer.class)
	public @interface JsonPersonId {

		static class PersonIdSerializer extends StdSerializer<Id<Person>> {

			protected PersonIdSerializer() {
				this(null);
			}

			protected PersonIdSerializer(Class<Id<Person>> vc) {
				super(vc);
			}

			@Override
			public void serialize(Id<Person> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
				gen.writeString(value.toString());
			}

		}

		static class PersonIdDeserializer extends StdDeserializer<Id<Person>> {

			protected PersonIdDeserializer() {
				this(null);
			}

			protected PersonIdDeserializer(Class<?> vc) {
				super(vc);
			}

			@Override
			public Id<Person> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
				JsonNode node = jp.getCodec().readTree(jp);
				return Id.createPersonId(node.asText());
			}

		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonSerialize(using = JsonLinkId.LinkIdSerializer.class)
	@JsonDeserialize(using = JsonLinkId.LinkIdDeserializer.class)
	public @interface JsonLinkId {

		static class LinkIdSerializer extends StdSerializer<Id<Link>> {

			protected LinkIdSerializer() {
				this(null);
			}

			protected LinkIdSerializer(Class<Id<Link>> vc) {
				super(vc);
			}

			@Override
			public void serialize(Id<Link> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
				gen.writeString(value.toString());
			}

		}

		static class LinkIdDeserializer extends StdDeserializer<Id<Link>> {

			protected LinkIdDeserializer() {
				this(null);
			}

			protected LinkIdDeserializer(Class<?> vc) {
				super(vc);
			}

			@Override
			public Id<Link> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
				JsonNode node = jp.getCodec().readTree(jp);
				return Id.createLinkId(node.asText());
			}

		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@JacksonAnnotationsInside
	@JsonSerialize(using = JsonNodeId.NodeIdSerializer.class)
	@JsonDeserialize(using = JsonNodeId.NodeIdDeserializer.class)
	public @interface JsonNodeId {

		static class NodeIdSerializer extends StdSerializer<Id<Node>> {

			protected NodeIdSerializer() {
				this(null);
			}

			protected NodeIdSerializer(Class<Id<Node>> vc) {
				super(vc);
			}

			@Override
			public void serialize(Id<Node> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
				gen.writeString(value.toString());
			}

		}

		static class NodeIdDeserializer extends StdDeserializer<Id<Node>> {

			protected NodeIdDeserializer() {
				this(null);
			}

			protected NodeIdDeserializer(Class<?> vc) {
				super(vc);
			}

			@Override
			public Id<Node> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
				JsonNode node = jp.getCodec().readTree(jp);
				return Id.createNodeId(node.asText());
			}

		}

	}

}
