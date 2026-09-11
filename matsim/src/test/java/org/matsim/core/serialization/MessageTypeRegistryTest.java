package org.matsim.core.serialization;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeRegistryTest {

	private final MessageTypeRegistry registry = MessageTypeRegistry.getInstance();

	@Test
	void findsConcreteEventClassesOnTheClasspath() {
		assertThat(registry.isRegistered(LinkEnterEvent.class)).isTrue();
		assertThat(registry.isRegistered(PersonArrivalEvent.class)).isTrue();
	}

	@Test
	void typeIdsAreStableAndRoundTrip() {
		int type = registry.getType(LinkEnterEvent.class);
		assertThat(type).isEqualTo(LinkEnterEvent.class.getName().hashCode());
		assertThat(registry.getType(type)).isEqualTo(LinkEnterEvent.class);
		assertThat(registry.hasType(type)).isTrue();
	}

	@Test
	void anyTypeMapsToEvent() {
		assertThat(registry.getType(Event.class)).isEqualTo(Message.ANY_TYPE);
		assertThat(registry.getType(Message.ANY_TYPE)).isEqualTo(Event.class);
		assertThat(registry.hasType(Message.ANY_TYPE)).isTrue();
	}

	@Test
	void assignableTypesIncludeSubclasses() {
		int[] eventTypes = registry.getAssignableTypes(Event.class);
		assertThat(eventTypes)
			.contains(registry.getType(PersonArrivalEvent.class))
			.contains(registry.getType(LinkEnterEvent.class));
	}
}
