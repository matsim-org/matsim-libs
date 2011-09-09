package playground.mzilske.freight.events;


public interface ShipmentPickedUpEventHandler extends CarrierEventHandler{
	public void handleEvent(ShipmentPickedUpEvent event);
}
