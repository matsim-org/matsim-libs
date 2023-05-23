package lsp.events;

import lsp.LSPResource;
import lsp.shipment.LSPShipment;
import lsp.usecase.TransshipmentHub;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class HandlinInHubStartEventCreator implements LogisticEventCreator{

    @Override
    public Event createEvent(Event event, Id<LSPShipment> lspShipmentId, Activity activity) {
        //TODO: This is just a first dummy implementation, KMT May'23
        return new HandlingInHubStartsEvent(event.getTime(), activity.getLinkId(), lspShipmentId, Id.create("DummyHubId", LSPResource.class));
    }
}
