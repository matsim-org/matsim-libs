/**
 * 
 */
package playground.mzilske.city2000w;

import org.apache.log4j.Logger;

import playground.mzilske.freight.TSPCostListener;
import playground.mzilske.freight.TSPShipment;


/**
 * @author stscr
 *
 */
public class DefaultLSPShipmentTracker implements TSPCostListener {
	private static Logger logger = Logger.getLogger(DefaultLSPShipmentTracker.class);
	
	@Override
	public void informCost(TSPShipment shipment, Double cost){
		logger.debug("TotalLogisticCost(Shipment="+shipment+")="+cost);
	}
}
