package demand.utilityFunctions;

import demand.demandObject.ShipperShipment;
import demand.offer.Offer;

public interface UtilityFunction {

	public String getName();
	public double getUtilityValue(Offer offer, ShipperShipment shipment);
	
}
