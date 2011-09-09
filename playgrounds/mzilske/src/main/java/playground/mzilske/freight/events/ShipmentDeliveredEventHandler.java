package playground.mzilske.freight.events;


public interface ShipmentDeliveredEventHandler extends CarrierEventHandler {
	public void handleEvent(ShipmentDeliveredEvent event);
}
