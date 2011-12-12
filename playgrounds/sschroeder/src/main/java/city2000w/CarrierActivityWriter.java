package city2000w;

import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEventHandler;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

public class CarrierActivityWriter implements ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler{

	private String filename;
	
	private boolean writerOpen = false;
	
	private BufferedWriter writer = null;
	
	public CarrierActivityWriter(String filename) {
		super();
		this.filename = filename;
	}

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
