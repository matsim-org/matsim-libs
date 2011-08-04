package freight;

import org.apache.log4j.Logger;

public class TlcCostFunction implements ShipperCostFunction{

	private static Logger logger = Logger.getLogger(TlcCostFunction.class);
	
	public double capitalCostRate = 0.01;
	
	@Override
	public double getCosts(CommodityFlow commodityFlow, int shipmentSize, int frequency, double transportationCostsPerShipment) {
		double inventory = getInventoryCost(commodityFlow, shipmentSize);
		double transportation = getTransportCost(commodityFlow, frequency, transportationCostsPerShipment);
		return inventory + transportation;
	}

	@Override
	public double getTransportCost(CommodityFlow commodityFlow, int frequency, double transportCostPerShipment) {
		return transportCostPerShipment * (double)frequency;
	}

	@Override
	public double getInventoryCost(CommodityFlow commodityFlow, int shipmentSize) {  
		return (double)shipmentSize/2.0 * capitalCostRate * commodityFlow.getValue();
	}

}
