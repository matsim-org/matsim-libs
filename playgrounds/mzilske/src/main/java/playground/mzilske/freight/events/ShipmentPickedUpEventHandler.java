package playground.mzilske.freight.events;


import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;

public interface ShipmentPickedUpEventHandler extends CarrierEventHandler{
	public void handleEvent(ShipmentPickedUpEvent event);
}
