package freight.api;

import freight.ShipperAgent;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;

public interface ShipperAgentFactory {
	public ShipperAgent createShipperAgent(ShipperAgentTracker shipperAgentTracker, ShipperImpl shipper);
}
