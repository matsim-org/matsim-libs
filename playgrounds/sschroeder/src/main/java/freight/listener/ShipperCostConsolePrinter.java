package freight.listener;

import org.apache.log4j.Logger;

import freight.ShipperAgent.DetailedCost;
import freight.ShipperAgent.TotalCost;

public class ShipperCostConsolePrinter implements ShipperDetailedCostListener, ShipperTotalCostListener{

	private static Logger logger = Logger.getLogger(ShipperCostConsolePrinter.class);
	
	@Override
	public void inform(DetailedCost detailedCost) {
		logger.info(detailedCost.shipperId + "; comFlow=" + detailedCost.comFlow + "; size=" + detailedCost.shipmentSize + "; freq=" + detailedCost.frequency + 
				"; tlc=" + detailedCost.tlc + "; transportation=" + detailedCost.transportation + "; inventory=" + detailedCost.inventory);
	}

	@Override
	public void inform(TotalCost totalCost) {
		logger.info(totalCost.shipperId + "; totCosts=" + totalCost.totCost);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

}
