package org.matsim.core.events.algorithms;

import org.matsim.api.core.v01.events.*;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.pb.*;
import org.matsim.core.utils.pb.ActivityFacilityId;
import org.matsim.core.utils.pb.ContentType;
import org.matsim.core.utils.pb.DepartureId;
import org.matsim.core.utils.pb.LinkId;
import org.matsim.core.utils.pb.PBFileHeader;
import org.matsim.core.utils.pb.PersonId;
import org.matsim.core.utils.pb.ProtoEvents;
import org.matsim.core.utils.pb.TransitLineId;
import org.matsim.core.utils.pb.TransitRouteId;
import org.matsim.core.utils.pb.VehicleId;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Event writer for protobuf format according to {@link org.matsim.core.utils.pb.Wireformat}
 */
public class EventWriterPB implements EventWriter, BasicEventHandler {

    private final OutputStream out;


    public EventWriterPB(OutputStream out) {

        this.out = out;

        PBFileHeader header = PBFileHeader.newBuilder()
                .setVersion(PBVersion.EVENTS)
                .setContentType(ContentType.EVENTS)
                .build();

        try {
            header.writeDelimitedTo(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public void closeFile() {
        try {
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void reset(int iteration) {
        // Nothing to do
    }

    @Override
    public void handleEvent(Event event) {
        try {
            convertEvent(event)
                    .writeDelimitedTo(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ProtoEvents.Event convertEvent(Event event) {

        ProtoEvents.Event.Builder builder = ProtoEvents.Event.newBuilder()
                .setTime(event.getTime());

        if (event instanceof GenericEvent) {
            builder.getGenericBuilder()
                    .setType(event.getEventType())
                    .putAllAttrs(event.getAttributes());
        } else if (event instanceof ActivityEndEvent) {
            builder.getActivityEndBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((ActivityEndEvent) event).getLinkId().toString()))
                    .setFacilityId(ActivityFacilityId.newBuilder().setId(((ActivityEndEvent) event).getFacilityId().toString()))
                    .setPersonId(PersonId.newBuilder().setId(((ActivityEndEvent) event).getPersonId().toString()))
                    .setActtype(((ActivityEndEvent) event).getActType());
        } else if (event instanceof ActivityStartEvent) {
            builder.getActivityStartBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((ActivityStartEvent) event).getLinkId().toString()))
                    .setFacilityId(ActivityFacilityId.newBuilder().setId(((ActivityStartEvent) event).getFacilityId().toString()))
                    .setPersonId(PersonId.newBuilder().setId(((ActivityStartEvent) event).getPersonId().toString()))
                    .setActtype(((ActivityStartEvent) event).getActType());
        } else if (event instanceof LinkEnterEvent) {
            builder.getLinkEnterBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((LinkEnterEvent) event).getLinkId().toString()))
                    .setVehicleId(VehicleId.newBuilder().setId(((LinkEnterEvent) event).getVehicleId().toString()));
        } else if (event instanceof LinkLeaveEvent) {
            builder.getLinkLeaveBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((LinkLeaveEvent) event).getLinkId().toString()))
                    .setVehicleId(VehicleId.newBuilder().setId(((LinkLeaveEvent) event).getVehicleId().toString()));
        } else if (event instanceof PersonArrivalEvent) {
            builder.getPersonalArrivalBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((PersonArrivalEvent) event).getLinkId().toString()))
                    .setLegMode(((PersonArrivalEvent) event).getLegMode())
                    .setPersonId(PersonId.newBuilder().setId(((PersonArrivalEvent) event).getPersonId().toString()));
        } else if (event instanceof PersonDepartureEvent) {
            builder.getPersonDepartureBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((PersonDepartureEvent) event).getLinkId().toString()))
                    .setLegMode(((PersonDepartureEvent) event).getLegMode())
                    .setPersonId(PersonId.newBuilder().setId(((PersonDepartureEvent) event).getPersonId().toString()));
        } else if (event instanceof PersonEntersVehicleEvent) {
            builder.getPersonEntersVehicleBuilder()
                    .setVehicleId(VehicleId.newBuilder().setId(((PersonEntersVehicleEvent) event).getVehicleId().toString()))
                    .setPersonId(PersonId.newBuilder().setId(((PersonEntersVehicleEvent) event).getPersonId().toString()));
        } else if (event instanceof PersonLeavesVehicleEvent) {
            builder.getPersonLeavesVehicleBuilder()
                    .setVehicleId(VehicleId.newBuilder().setId(((PersonLeavesVehicleEvent) event).getVehicleId().toString()))
                    .setPersonId(PersonId.newBuilder().setId(((PersonLeavesVehicleEvent) event).getPersonId().toString()));
        } else if (event instanceof PersonMoneyEvent) {
            builder.getPersonMoneyBuilder()
                    .setPersonId(PersonId.newBuilder().setId(((PersonMoneyEvent) event).getPersonId().toString()))
                    .setAmount(((PersonMoneyEvent) event).getAmount())
                    .setPurpose(((PersonMoneyEvent) event).getPurpose())
                    .setTransactionPartner(((PersonMoneyEvent) event).getTransactionPartner());
        } else if (event instanceof PersonStuckEvent) {
            builder.getPersonStuckBuilder()
                    .setLinkId(LinkId.newBuilder().setId(((PersonStuckEvent) event).getLinkId().toString()))
                    .setPersonId(PersonId.newBuilder().setId(((PersonStuckEvent) event).getPersonId().toString()))
                    .setLegMode(((PersonStuckEvent) event).getLegMode());
        } else if (event instanceof TransitDriverStartsEvent) {
            builder.getTransitDriverStartsBuilder()
                    .setDriverId(PersonId.newBuilder().setId(((TransitDriverStartsEvent) event).getDriverId().toString()))
                    .setVehicleId(VehicleId.newBuilder().setId(((TransitDriverStartsEvent) event).getVehicleId().toString()))
                    .setTransitRouteId(TransitRouteId.newBuilder().setId(((TransitDriverStartsEvent) event).getTransitRouteId().toString()))
                    .setTransitLineId(TransitLineId.newBuilder().setId(((TransitDriverStartsEvent) event).getTransitLineId().toString()))
                    .setDepartureId(DepartureId.newBuilder().setId(((TransitDriverStartsEvent) event).getDepartureId().toString()));
        } else if (event instanceof VehicleAbortsEvent) {
            builder.getVehicleAbortsBuilder()
                    .setVehicleId(VehicleId.newBuilder().setId(((VehicleAbortsEvent) event).getVehicleId().toString()))
                    .setLinkId(LinkId.newBuilder().setId(((VehicleAbortsEvent) event).getLinkId().toString()));
        } else if (event instanceof VehicleEntersTrafficEvent) {
            builder.getVehicleEntersTrafficBuilder()
                    .setDriverId(PersonId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getPersonId().toString()))
                    .setLinkId(LinkId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getLinkId().toString()))
                    .setVehicleId(VehicleId.newBuilder().setId(((VehicleEntersTrafficEvent) event).getVehicleId().toString()))
                    .setNetworkMode(((VehicleEntersTrafficEvent) event).getNetworkMode())
                    .setRelativePositionOnLink(((VehicleEntersTrafficEvent) event).getRelativePositionOnLink());
        } else if (event instanceof VehicleLeavesTrafficEvent) {
            builder.getVehicleLeavesTrafficBuilder()
                    .setDriverId(PersonId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getPersonId().toString()))
                    .setLinkId(LinkId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getLinkId().toString()))
                    .setVehicleId(VehicleId.newBuilder().setId(((VehicleLeavesTrafficEvent) event).getVehicleId().toString()))
                    .setNetworkMode(((VehicleLeavesTrafficEvent) event).getNetworkMode())
                    .setRelativePositionOnLink(((VehicleLeavesTrafficEvent) event).getRelativePositionOnLink());
        } else {
            // TODO: should warn here
            builder.getGenericBuilder()
                    .setType(event.getEventType())
                    .putAllAttrs(event.getAttributes());
        }

        return builder.build();
    }
}
