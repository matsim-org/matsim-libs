package org.matsim.contrib.protobuf;

import com.google.protobuf.Descriptors;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.pb.PBFileHeader;
import org.matsim.core.utils.pb.ProtoEvents;
import org.matsim.core.utils.pb.ProtoId;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EventWriterPBTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void writer() throws IOException {

//        File file = tmp.newFile("output.pb");
        File file = new File( utils.getOutputDirectory() + "events.pb" );
        OutputStream out = IOUtils.getOutputStream(file.toURI().toURL(), false);

        EventWriterPB writer = new EventWriterPB(out);

        for (int i = 1; i <= 2000; i++) {
            writer.handleEvent(new GenericEvent(String.valueOf(i), i * i));
        }

        writer.closeFile();
        assertThat(file)
                .exists()
                .isNotEmpty();

        // read this back in:

        // (one could argue that reading should be a separate test.  On the other hand, write-read tests and read-write tests are quite normal and quite
        // plausible.  This one here, however, is not complete; it should check if we can reconstruct the original material.)

        final FileInputStream fileInputStream = new FileInputStream( utils.getOutputDirectory() + "/events.pb" );
        {
            PBFileHeader header = PBFileHeader.parseDelimitedFrom( fileInputStream );
            for( Map.Entry<Descriptors.FieldDescriptor, Object> entry : header.getAllFields().entrySet() ){
                System.out.println( "key=" + entry.getKey() + "; value=" + entry.getValue() );
            }
        }
//        {
//            PBFileHeader header = PBFileHeader.parseDelimitedFrom( fileInputStream );
//            for( Map.Entry<Descriptors.FieldDescriptor, Object> entry : header.getAllFields().entrySet() ){
//                System.out.println( "key=" + entry.getKey() + "; value=" + entry.getValue() );
//            }
//        }
        {
            ProtoEvents.EventBatch batch = ProtoEvents.EventBatch.parseDelimitedFrom( fileInputStream );
            for( ProtoEvents.Event event : batch.getEventsList() ){
                System.out.println( "event=" + event );
            }
        }

        // the following does not read any additional material.  I am, in fact, a bit surprised that it does not cause an error.  Looks like it is
        // possible to read non-existing containers as "empty" containers.  However, reading the header twice (as above) causes this one here to fail.  --???
//        {
//            ProtoEvents.EventBatch batch = ProtoEvents.EventBatch.parseDelimitedFrom( fileInputStream );
//            for( ProtoEvents.Event event : batch.getEventsList() ){
//                System.out.println( "event=" + event );
//            }
//        }


    }

	@Test
	void convertEvent() {

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
	void convertId() {

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
