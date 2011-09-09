package freight;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import freight.listener.ShipperEventImpl;

public class DetailedCostStatusEvent extends ShipperEventImpl implements Event{
	private CommodityFlow comFlow;
	private int shipmentSize;
	private int frequency;
	private double tlc;
	private double transportation;
	private double inventory;
	
	public DetailedCostStatusEvent(Id shipperId, CommodityFlow comFlow,
			int shipmentSize, int frequency, double tlc,
			double transportation, double inventory) {
		super(shipperId);
		this.comFlow = comFlow;
		this.shipmentSize = shipmentSize;
		this.frequency = frequency;
		this.tlc = tlc;
		this.transportation = transportation;
		this.inventory = inventory;
	}
	public CommodityFlow getComFlow() {
		return comFlow;
	}
	public int getShipmentSize() {
		return shipmentSize;
	}
	public int getFrequency() {
		return frequency;
	}
	public double getTlc() {
		return tlc;
	}
	public double getTransportation() {
		return transportation;
	}
	public double getInventory() {
		return inventory;
	}
	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}
}