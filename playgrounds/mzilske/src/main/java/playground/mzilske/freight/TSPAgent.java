package playground.mzilske.freight;

import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;
import playground.mzilske.freight.carrier.Shipment;

public interface TSPAgent {

	public abstract void reset();

	public abstract void scoreSelectedPlan();

	public abstract TSPOffer requestService(Id from, Id to, int size,
			double startPickup, double endPickup, double startDelivery,
			double endDelivery);


	public abstract void shipmentPickedUp(Shipment shipment, double time);

	public abstract void shipmentDelivered(Shipment shipment, double time);

	public abstract void calculateCosts();

	public abstract List<CarrierContract> getCarrierContracts();

	public abstract boolean hasShipment(Shipment shipment);

	public abstract Id getId();

	public abstract List<CarrierOffer> getCarrierOffers(TSPOffer offer);

	public abstract void informShipperContractAccepted(Contract contract);

	public abstract void informShipperContractCanceled(Contract contract);

	public abstract void informCarrierContractAccepted(Contract contract);

	public abstract void informCarrierContractCanceled(Contract contract);

	public abstract void informChainRemoved(TransportChain chain);

	public abstract void informChainAdded(TransportChain chain);

}