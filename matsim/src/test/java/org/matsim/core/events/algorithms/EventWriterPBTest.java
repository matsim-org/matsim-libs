package org.matsim.core.events.algorithms;

import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.utils.pb.ProtoEvents;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import static org.assertj.core.api.Assertions.assertThat;

public class EventWriterPBTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();


    @Test
    public void convertEvent() {

        // test few sample events
        GenericEvent event = new GenericEvent("test", 10.0);
        event.getAttributes().put("sample", "15");

        assertThat(EventWriterPB.convertEvent(event))
                .hasFieldOrPropertyWithValue("time", 10.0)
                .extracting(ProtoEvents.Event::getGeneric)
                .hasFieldOrPropertyWithValue("type", "test")
                .extracting(ProtoEvents.GenericEvent::getAttrsMap).has(new Condition<>(m -> m.get("sample").equals("15"), "contains attribute"));

        assertThat(EventWriterPB.convertEvent(new PersonDepartureEvent(15.0, Id.createPersonId(15), Id.createLinkId(20), "test")))
                .hasFieldOrPropertyWithValue("time", 15.0)
                .extracting(ProtoEvents.Event::getPersonDeparture)
                .hasFieldOrPropertyWithValue("legMode", "test")
                .has(new Condition<>(e -> e.getPersonId().getId().equals("15") && e.getLinkId().getId().equals("20"),"Ids correct"));
    }

    @Test
    public void convertId() {

        Id<Vehicle> id = Id.createVehicleId(123);

        ProtoEvents.LinkEnterEvent msg = ProtoEvents.LinkEnterEvent
                .newBuilder()
                .setLinkId(EventWriterPB.convertId(id))
                .build();

        assertThat(msg.hasLinkId());


        msg = ProtoEvents.LinkEnterEvent
                .newBuilder()
                .setLinkId(EventWriterPB.convertId(null))
                .build();


        assertThat(!msg.hasLinkId());
    }

}
