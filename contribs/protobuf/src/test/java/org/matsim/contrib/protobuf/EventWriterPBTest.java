package org.matsim.contrib.protobuf;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.pb.ProtoEvents;
import org.matsim.core.utils.pb.ProtoId;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class EventWriterPBTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void writer() throws IOException {

        File file = tmp.newFile("output.pb");
        OutputStream out = IOUtils.getOutputStream(file.toURI().toURL(), false);

        EventWriterPB writer = new EventWriterPB(out);

        for (int i = 1; i <= 2000; i++) {
            writer.handleEvent(new GenericEvent(String.valueOf(i), i * i));
        }

        writer.closeFile();
        assertThat(file)
                .exists()
                .isNotEmpty();
    }

    @Test
    public void convertEvent() {

        // test few sample events
        GenericEvent event = new GenericEvent("test", 10.0);
        event.getAttributes().put("sample", "15");

        assertThat(EventWriterPB.convertEvent(event))
                .hasFieldOrPropertyWithValue("time", 10.0)
                .extracting(ProtoEvents.Event::getGeneric)
                .hasFieldOrPropertyWithValue("type", "test")
                .extracting(ProtoEvents.GenericEvent::getAttrsMap)
                .has(new Condition<>(m -> m.get("sample").equals("15"), "contains custom attribute"))
                .has(new Condition<>(m -> !m.containsKey(Event.ATTRIBUTE_TIME) && !m.containsKey(Event.ATTRIBUTE_X), "does not contain common attributes"));

        assertThat(EventWriterPB.convertEvent(new PersonDepartureEvent(15.0, Id.createPersonId(15), Id.createLinkId(20), "test", "test")))
                .hasFieldOrPropertyWithValue("time", 15.0)
                .extracting(ProtoEvents.Event::getPersonDeparture)
                .hasFieldOrPropertyWithValue("legMode", "test")
                .has(new Condition<>(e -> e.getPersonId().getId().equals("15") && e.getLinkId().getId().equals("20"), "Ids correct"));
    }

    @Test
    public void convertId() {

        Id<Vehicle> id = Id.createVehicleId(123);

        ProtoEvents.LinkEnterEvent msg = ProtoEvents.LinkEnterEvent
                .newBuilder()
                .setLinkId(EventWriterPB.convertId(id))
                .build();

        Assertions.assertThat(msg.hasLinkId());

        msg = ProtoEvents.LinkEnterEvent
                .newBuilder()
                .setLinkId(EventWriterPB.convertId(null))
                .build();

        Assertions.assertThat(!msg.hasLinkId());

        // check that empty string is a valid id
        msg = ProtoEvents.LinkEnterEvent.newBuilder()
                .setLinkId(ProtoId.newBuilder().setId(""))
                .build();

        Assertions.assertThat(msg.hasLinkId());

    }

}
