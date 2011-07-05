package vrp.algorithms.ruinAndRecreate.recreation;

import vrp.algorithms.ruinAndRecreate.basics.Shipment;

public class RecreationEvent {
	
	private Shipment shipment;
	
	private double cost;

	public RecreationEvent(Shipment shipment, double cost) {
		super();
		this.shipment = shipment;
		this.cost = cost;
	}

	public Shipment getShipment() {
		return shipment;
	}

	public double getCost() {
		return cost;
	}
	
	
}
