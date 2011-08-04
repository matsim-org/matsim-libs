package freight;

public interface ShipperAgentFactory {
	public ShipperAgent createShipperAgent(ShipperAgentTracker shipperAgentTracker, ShipperImpl shipper);
}
