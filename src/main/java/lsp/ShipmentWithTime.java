package lsp;


import lsp.shipment.LSPShipment;

public class ShipmentWithTime{
	private final LSPShipment shipment;
	private final double time;

	public ShipmentWithTime( double time , LSPShipment shipment ) {
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
