package freight.listener;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

public class ShipperTotalCostStatusEvent extends ShipperEventImpl implements Event{

	private double totalCosts;

	public ShipperTotalCostStatusEvent(Id shipperId, double totalCosts) {
		super(shipperId);
		this.totalCosts = totalCosts;
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

	public double getTotalCosts() {
		return totalCosts;
	}

}
