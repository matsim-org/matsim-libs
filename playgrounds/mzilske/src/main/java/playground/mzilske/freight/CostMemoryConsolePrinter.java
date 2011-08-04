package playground.mzilske.freight;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CostMemoryImpl.CostTableKey;

public class CostMemoryConsolePrinter implements CarrierCostMemoryListener {

	private Logger logger = Logger.getLogger(CostMemoryConsolePrinter.class);
	
	@Override
	public void inform(Id carrierId, CostMemory costMemory) {
		logger.info("carrierId=" + carrierId);
		CostMemoryImpl costMemoryImpl = (CostMemoryImpl)costMemory;
		for(CostTableKey key : costMemoryImpl.getCostMap().keySet()){
			logger.info(key + "; cost=" + costMemoryImpl.getCostMap().get(key));
		}
	}

}
