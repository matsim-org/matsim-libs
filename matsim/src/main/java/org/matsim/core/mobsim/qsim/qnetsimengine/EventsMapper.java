package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.facilities.ActivityFacility;

import java.util.Map;

public class EventsMapper {

    public static Event map(EventDto eventDto) {
        Map<String, String> attributes = eventDto.getAttributes();

        Event event;
        switch (eventDto.getType()) {
            case PersonEntersVehicleEvent.EVENT_TYPE:
                event = new PersonEntersVehicleEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(PersonEntersVehicleEvent.ATTRIBUTE_PERSON)),
                        Id.createVehicleId(attributes.get(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE))
                );
                break;

            case PersonLeavesVehicleEvent.EVENT_TYPE:
                event = new PersonLeavesVehicleEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON)),
                        Id.createVehicleId(attributes.get(PersonLeavesVehicleEvent.ATTRIBUTE_VEHICLE))
                );
                break;

            case ActivityEndEvent.EVENT_TYPE:
                event = new ActivityEndEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(ActivityEndEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(ActivityEndEvent.ATTRIBUTE_LINK)),
                        idOrNull(attributes.get(ActivityEndEvent.ATTRIBUTE_FACILITY), ActivityFacility.class),
                        attributes.get(ActivityEndEvent.ATTRIBUTE_ACTTYPE)
                );
                break;

            case ActivityStartEvent.EVENT_TYPE:
                event = new ActivityStartEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(ActivityStartEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(ActivityStartEvent.ATTRIBUTE_LINK)),
                        idOrNull(attributes.get(ActivityStartEvent.ATTRIBUTE_FACILITY), ActivityFacility.class),
                        attributes.get(ActivityStartEvent.ATTRIBUTE_ACTTYPE),
                        new Coord(
                                Double.parseDouble(attributes.get(ActivityStartEvent.ATTRIBUTE_X)),
                                Double.parseDouble(attributes.get(ActivityStartEvent.ATTRIBUTE_Y)))
                );
                break;

            case PersonArrivalEvent.EVENT_TYPE:
                event = new PersonArrivalEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(PersonArrivalEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(PersonArrivalEvent.ATTRIBUTE_LINK)),
                        attributes.get(PersonArrivalEvent.ATTRIBUTE_LEGMODE)
                );
                break;

            case PersonDepartureEvent.EVENT_TYPE:
                event = new PersonDepartureEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(PersonDepartureEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(PersonDepartureEvent.ATTRIBUTE_LINK)),
                        attributes.get(PersonDepartureEvent.ATTRIBUTE_LEGMODE)
                );
                break;

            case LinkEnterEvent.EVENT_TYPE:
                event = new LinkEnterEvent(eventDto.getTime(),
                        Id.createVehicleId(attributes.get(LinkEnterEvent.ATTRIBUTE_VEHICLE)),
                        Id.createLinkId(attributes.get(LinkEnterEvent.ATTRIBUTE_LINK))
                );
                break;

            case LinkLeaveEvent.EVENT_TYPE:
                event = new LinkLeaveEvent(eventDto.getTime(),
                        Id.createVehicleId(attributes.get(LinkLeaveEvent.ATTRIBUTE_VEHICLE)),
                        Id.createLinkId(attributes.get(LinkLeaveEvent.ATTRIBUTE_LINK))
                );
                break;

            case TeleportationArrivalEvent.EVENT_TYPE:
                event = new TeleportationArrivalEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(TeleportationArrivalEvent.ATTRIBUTE_PERSON)),
                        Double.parseDouble(attributes.get(TeleportationArrivalEvent.ATTRIBUTE_DISTANCE)),
                        attributes.get(TeleportationArrivalEvent.ATTRIBUTE_MODE));
                break;

            case VehicleEntersTrafficEvent.EVENT_TYPE:
                event = new VehicleEntersTrafficEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(VehicleEntersTrafficEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(VehicleEntersTrafficEvent.ATTRIBUTE_LINK)),
                        Id.createVehicleId(attributes.get(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE)),
                        attributes.get(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE),
                        Double.parseDouble(attributes.get(VehicleEntersTrafficEvent.ATTRIBUTE_POSITION)));
                break;

            case VehicleLeavesTrafficEvent.EVENT_TYPE:
                event = new VehicleLeavesTrafficEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(VehicleLeavesTrafficEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(VehicleLeavesTrafficEvent.ATTRIBUTE_LINK)),
                        Id.createVehicleId(attributes.get(VehicleLeavesTrafficEvent.ATTRIBUTE_VEHICLE)),
                        attributes.get(VehicleLeavesTrafficEvent.ATTRIBUTE_NETWORKMODE),
                        Double.parseDouble(attributes.get(VehicleLeavesTrafficEvent.ATTRIBUTE_POSITION)));
                break;

            case PersonStuckEvent.EVENT_TYPE:
                event = new PersonStuckEvent(eventDto.getTime(),
                        Id.createPersonId(attributes.get(PersonStuckEvent.ATTRIBUTE_PERSON)),
                        Id.createLinkId(attributes.get(PersonStuckEvent.ATTRIBUTE_LINK)),
                        attributes.get(PersonStuckEvent.ATTRIBUTE_LEGMODE));
                break;

            case VehicleAbortsEvent.EVENT_TYPE:
                event = new VehicleAbortsEvent(eventDto.getTime(),
                        Id.createVehicleId(attributes.get(VehicleAbortsEvent.ATTRIBUTE_VEHICLE)),
                        Id.createLinkId(attributes.get(VehicleAbortsEvent.ATTRIBUTE_LINK)));
                break;


            default:
                throw new IllegalArgumentException("Event unknown to mapper " + eventDto.getType());
        }
        return event;
    }

    private static <T> Id<T> idOrNull(String id, Class<T> type) {
        return id != null ? Id.create(id, type) : null;
    }
}
