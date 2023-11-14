package org.matsim.freight.logistics.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.shipment.LSPShipment;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class HandlingInHubStartsEventCreator implements LspEventCreator {

    @Override
    public Event createEvent(Event event, Id<LSPShipment> lspShipmentId, Activity activity) {
        //TODO: This is just a first dummy implementation, KMT May'23
        //TODO: expHandlingDuration is temporarily set to minValue. Will decide later wether this creater is needed anyways or not. KMT jul'23
        return new HandlingInHubStartsEvent(event.getTime(), activity.getLinkId(), lspShipmentId, Id.create("DummyHubId", LSPResource.class), Double.MIN_VALUE);
    }
}
