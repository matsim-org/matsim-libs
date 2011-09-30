package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;

public interface CarrierTradingAgent {
	
	abstract void reset();

	abstract void informOfferRejected(CarrierOffer offer);

	abstract void informOfferAccepted(CarrierContract contract);

	abstract void informTSPContractAccepted(CarrierContract contract);

	abstract void informTSPContractCanceled(CarrierContract contract);

	abstract CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery);

	abstract Object getId();

}
