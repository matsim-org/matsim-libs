package playground.mzilske.freight.events;


import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;

public interface ShipmentDeliveredEventHandler extends CarrierEventHandler {
	public void handleEvent(ShipmentDeliveredEvent event);
}
