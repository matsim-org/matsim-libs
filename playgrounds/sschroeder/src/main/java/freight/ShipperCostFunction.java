package freight;

public interface ShipperCostFunction {

	public double getCosts(CommodityFlow commodityFlow, int shipmentSize, int frequency, double transportationCostsPerShipment);

	public double getTransportCost(CommodityFlow commodityFlow, int frequency, double transportationCostPerShipment);

	public double getInventoryCost(CommodityFlow commodityFlow,int shipmentSize);
	
}
