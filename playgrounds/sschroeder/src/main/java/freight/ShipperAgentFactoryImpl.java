package freight;

public class ShipperAgentFactoryImpl implements ShipperAgentFactory {

	@Override
	public ShipperAgent createShipperAgent(ShipperAgentTracker shipperAgentTracker, ShipperImpl shipper) {
		ShipperAgent shipperAgent = new ShipperAgent(shipper);
		shipperAgent.setCostFunction(new TlcCostFunction());
		shipperAgent.setShipperAgentTracker(shipperAgentTracker);
		return shipperAgent;
	}

}
