package org.matsim.api.core.v01;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.IdAnnotations.JsonLinkId;
import org.matsim.api.core.v01.IdAnnotations.JsonNodeId;
import org.matsim.api.core.v01.IdAnnotations.JsonPersonId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class IdAnnotationsTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testRecordJsonIds() throws JsonProcessingException {
		RecordWithIds recordWithIds1 = new RecordWithIds(
				Id.createPersonId("person"),
				Id.createLinkId("link"),
				Id.createNodeId("node"));

		String s = objectMapper.writeValueAsString(recordWithIds1);
		RecordWithIds recordWithIds2 = objectMapper.readValue(s, RecordWithIds.class);

		Assert.assertEquals(recordWithIds1, recordWithIds2);
	}

	@Test
	public void testRecordJsonIdsWithNull() throws JsonProcessingException {
		RecordWithIds recordWithIds1 = new RecordWithIds(null, null, null);

		String s = objectMapper.writeValueAsString(recordWithIds1);
		RecordWithIds recordWithIds2 = objectMapper.readValue(s, RecordWithIds.class);

		Assert.assertEquals(recordWithIds1, recordWithIds2);
	}

	@Test
	public void testClassJsonIds() throws JsonProcessingException {
		ClassWithIds classWithIds1 = new ClassWithIds(
				Id.createPersonId("person"),
				Id.createLinkId("link"),
				Id.createNodeId("node"));

		String s = objectMapper.writeValueAsString(classWithIds1);
		ClassWithIds classWithIds2 = objectMapper.readValue(s, ClassWithIds.class);

		Assert.assertEquals(classWithIds1, classWithIds2);
	}

	@Test
	public void testClassJsonIdsWithNull() throws JsonProcessingException {
		ClassWithIds classWithIds1 = new ClassWithIds(null, null, null);

		String s = objectMapper.writeValueAsString(classWithIds1);
		ClassWithIds classWithIds2 = objectMapper.readValue(s, ClassWithIds.class);

		Assert.assertEquals(classWithIds1, classWithIds2);
	}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	private static record RecordWithIds(
			@JsonPersonId Id<Person> personId,
			@JsonLinkId Id<Link> linkId,
			@JsonNodeId Id<Node> nodeId) {
	};

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	private static class ClassWithIds {

		@JsonPersonId
		Id<Person> personId;

		@JsonLinkId
		Id<Link> linkId;

		@JsonNodeId
		Id<Node> nodeId;

		ClassWithIds() {
			// for deserialization
		}

		ClassWithIds(Id<Person> personId, Id<Link> linkId, Id<Node> nodeId) {
			this.personId = personId;
			this.linkId = linkId;
			this.nodeId = nodeId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o instanceof ClassWithIds classWithIds) {
				return Objects.equals(personId, classWithIds.personId)
						&& Objects.equals(linkId, classWithIds.linkId)
						&& Objects.equals(nodeId, classWithIds.nodeId);
			}
			return false;
		}

	};

}
