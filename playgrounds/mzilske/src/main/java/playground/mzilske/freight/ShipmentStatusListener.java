package playground.mzilske.freight;

public interface ShipmentStatusListener {

	public void shipmentPickedUp(Shipment shipment, double time);

	public void shipmentDelivered(Shipment shipment, double time);

}
