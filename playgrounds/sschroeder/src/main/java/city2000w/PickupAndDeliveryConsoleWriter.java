package city2000w;

import org.apache.log4j.Logger;

import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentDeliveredEventHandler;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEventHandler;

public class PickupAndDeliveryConsoleWriter implements ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler{

	private static Logger logger = Logger.getLogger(PickupAndDeliveryConsoleWriter.class);
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		logger.info("shipment delivered " + event.getTime() + " " + event.getShipment());
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		logger.info("shipment picked up " + event.getTime() + " " + event.getShipment());
	}

}
