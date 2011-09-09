package city2000w;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentDeliveredEventHandler;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEventHandler;

public class CarrierActivityWriter implements ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler{

	private String filename;
	
	private boolean writerOpen = false;
	
	private BufferedWriter writer = null;
	
	public CarrierActivityWriter(String filename) {
		super();
		this.filename = filename;
	}

	@Override
	public void finish() {
		closeWriter();
		
	}

	private void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		if(!writerOpen){
			openWriter();
		}
		try {
			writer.write(event.getTime() + ";" + "delivery" + ";" + event.getCarrierId() + ";" + event.getDriverId() + ";" + event.getShipment().getFrom() + ";" + event.getShipment().getTo() + ";" + 
					event.getShipment().getSize() + ";" + event.getShipment().getPickupTimeWindow().getStart() + ";" + event.getShipment().getPickupTimeWindow().getEnd() + ";" +
					event.getShipment().getDeliveryTimeWindow().getStart() + ";" + event.getShipment().getDeliveryTimeWindow().getEnd() + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void openWriter() {
		writer = IOUtils.getBufferedWriter(filename);
		writerOpen = true;
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		if(!writerOpen){
			openWriter();
		}
		try {
			writer.write(event.getTime() + ";" + "pickup" + ";" + event.getCarrierId() + ";" + event.getDriverId() + ";" + event.getShipment().getFrom() + ";" + event.getShipment().getTo() + ";" + 
					event.getShipment().getSize() + ";" + event.getShipment().getPickupTimeWindow().getStart() + ";" + event.getShipment().getPickupTimeWindow().getEnd() + ";" +
					event.getShipment().getDeliveryTimeWindow().getStart() + ";" + event.getShipment().getDeliveryTimeWindow().getEnd() + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
