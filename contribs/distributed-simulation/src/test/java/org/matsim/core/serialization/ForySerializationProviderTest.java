package org.matsim.core.serialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;

import java.nio.ByteBuffer;
import java.util.List;

class ForySerializationProviderTest {

	private final SerializationProvider provider = new ForySerializationProvider(MessageTypeRegistry.getInstance());

	@Test
	void serializeEvents() {

		List<Event> events = List.of(
			new PersonArrivalEvent(10, Id.createPersonId("p1"), Id.createLinkId("link"), TransportMode.car),
			new PersonDepartureEvent(20, Id.createPersonId("person"), Id.createLinkId("l2"), TransportMode.bike, TransportMode.bike),
			new LinkEnterEvent(20, Id.createVehicleId("v1"), Id.createLinkId("l1")),
			new LinkLeaveEvent(30, Id.createVehicleId("v2"), Id.createLinkId("l2")),
			new PersonStuckEvent(40, Id.createPersonId("p2"), Id.createLinkId("link2"), TransportMode.walk)
		);

		for (Event event : events) {

			var bytes = provider.serialize(event);

			ByteBuffer buf = ByteBuffer.wrap(bytes);

			Message result = provider.deserialize(buf, event.getType());

			Assertions.assertEquals(event, result);
			Assertions.assertEquals(bytes.length, buf.position(), "deserialize must advance the buffer position");
		}
	}
}
