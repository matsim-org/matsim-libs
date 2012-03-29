package playground.mzilske.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Contract;
import org.matsim.contrib.freight.carrier.Offer;
import org.matsim.contrib.freight.carrier.Shipment;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.trade.Service;

import playground.mzilske.freight.ContractImpl;

public class OfferUtils {

	public static Service createService(Shipment shipment) {
		return new Service(shipment.getFrom(),shipment.getTo(),shipment.getSize(),shipment.getPickupTimeWindow().getStart(),shipment.getPickupTimeWindow().getEnd(),
				shipment.getDeliveryTimeWindow().getStart(),shipment.getDeliveryTimeWindow().getEnd());
	}

	public static Service createService(Id from, Id to, int size,TimeWindow pickupTW, TimeWindow deliveryTW) {
		return new Service(from,to,size,pickupTW.getStart(),pickupTW.getEnd(),deliveryTW.getStart(),deliveryTW.getEnd());
	}
	
	public static Contract createContract(Id buyer, Id seller, Shipment shipment, Offer offer){
		return new ContractImpl(buyer,seller,shipment,offer);
	}
}
