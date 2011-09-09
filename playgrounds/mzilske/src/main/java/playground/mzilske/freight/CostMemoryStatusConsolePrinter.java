package playground.mzilske.freight;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CostMemoryImpl.CostTableKey;
import playground.mzilske.freight.api.CarrierCostMemoryStatusHandler;
import playground.mzilske.freight.events.CostMemoryStatusEvent;

public class CostMemoryStatusConsolePrinter implements CarrierCostMemoryStatusHandler {

	private Logger logger = Logger.getLogger(CostMemoryStatusConsolePrinter.class);
	
	@Override
	public void inform(Id carrierId, CostMemory costMemory) {
		logger.info("carrierId=" + carrierId);
		CostMemoryImpl costMemoryImpl = (CostMemoryImpl)costMemory;
		for(CostTableKey key : costMemoryImpl.getCostMap().keySet()){
			logger.info(key + "; cost=" + costMemoryImpl.getCostMap().get(key));
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CostMemoryStatusEvent event) {
		logger.info("carrierId=" + event.getCarrierId());
		CostMemoryImpl costMemoryImpl = (CostMemoryImpl)event.getCostMemory();
		for(CostTableKey key : costMemoryImpl.getCostMap().keySet()){
			logger.info(key + "; cost=" + costMemoryImpl.getCostMap().get(key));
		}
		
	}

}
