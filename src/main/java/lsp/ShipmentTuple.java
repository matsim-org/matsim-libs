package lsp;


import lsp.shipment.LSPShipment;

public class ShipmentTuple {
	private LSPShipment shipment;
	private double time;

	public ShipmentTuple(double time ,LSPShipment shipment) {
		this.shipment= shipment;
		this.time = time;
	}

	public LSPShipment getShipment() {
		return shipment;
	}

	public double getTime() {
		return time;
	}

}
