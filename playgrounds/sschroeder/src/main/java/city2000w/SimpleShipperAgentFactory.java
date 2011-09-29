package city2000w;

import freight.ShipperAgentImpl;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.TlcCostFunction;
import freight.api.ShipperAgentFactory;

public class SimpleShipperAgentFactory implements ShipperAgentFactory {

	@Override
	public ShipperAgentImpl createShipperAgent(ShipperAgentTracker shipperAgentTracker, ShipperImpl shipper) {
		ShipperAgentImpl shipperAgent = new ShipperAgentImpl(shipper);
		shipperAgent.setCostFunction(new TlcCostFunction());
		shipperAgent.setShipperAgentTracker(shipperAgentTracker);
		return shipperAgent;
	}

}
