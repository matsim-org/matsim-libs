package freight.listener;

import freight.DetailedCostStatusEvent;
import org.apache.log4j.Logger;

public class ShipperCostConsolePrinter implements ShipperDetailedCostStatusHandler, ShipperTotalCostStatusHandler{

	private static Logger logger = Logger.getLogger(ShipperCostConsolePrinter.class);

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ShipperTotalCostStatusEvent event) {
		logger.info(event.getShipperId()+ "; totCosts=" + event.getTotalCosts());
		
	}

	@Override
	public void handleEvent(DetailedCostStatusEvent event) {
		logger.info(event.getShipperId() + "; comFlow=" + event.getComFlow() + "; size=" + event.getShipmentSize() + "; freq=" + event.getFrequency() + 
				"; tlc=" + event.getTlc() + "; transportation=" + event.getTransportation() + "; inventory=" + event.getInventory());
	}

}
