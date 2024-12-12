package org.matsim.core.serialization;

import org.apache.fury.memory.MemoryBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;

import java.io.IOException;
import java.util.List;

class SerializationProviderTest {

	private final SerializationProvider provider = new SerializationProvider();

	@Test
	void serializeEvents() throws IOException {

		List<Event> events = List.of(
			new PersonArrivalEvent(10, Id.createPersonId("p1"), Id.createLinkId("link"), TransportMode.car),
			new PersonDepartureEvent(20, Id.createPersonId("person"), Id.createLinkId("l2"), TransportMode.bike, TransportMode.bike),
			new LinkEnterEvent(20, Id.createVehicleId("v1"), Id.createLinkId("l1")),
			new LinkLeaveEvent(30, Id.createVehicleId("v2"), Id.createLinkId("l2")),
			new PersonStuckEvent(40, Id.createPersonId("p2"), Id.createLinkId("link2"), TransportMode.walk)
		);

		for (Event event : events) {

			FuryBufferParser parser = provider.getFuryParser(event.getType());

			byte[] bytes = provider.getFury().serializeJavaObject(event);

			MemoryBuffer buf = MemoryBuffer.fromByteArray(bytes);

			Message result = parser.parse(buf);

			Assertions.assertEquals(event, result);
		}

	}
}
