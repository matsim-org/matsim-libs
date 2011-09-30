package playground.mzilske.freight;

import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;

public class CarrierTradingAgentImpl implements CarrierTradingAgent {

	private static Logger logger;
	private Carrier carrier;

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void informOfferRejected(CarrierOffer offer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void informOfferAccepted(CarrierContract contract) {
		// TODO Auto-generated method stub

	}


	@Override
	public void informTSPContractAccepted(CarrierContract contract) {
				logger.info("i am " + contract.getOffer().getId() + " and this is my new contract :)). buyer: " + contract.getBuyer() + "; seller: " + contract.getSeller());
				carrier.getContracts().add(contract);
				carrier.getNewContracts().add(contract);
	//			newContracts.add(contract);
			}

	@Override
	public void informTSPContractCanceled(CarrierContract contract) {
				logger.info("i am " + contract.getOffer().getId() + " and my contract was canceled ;)). offer: " + contract.getOffer().getPrice());	
	//			oldContracts.add(contract);
				carrier.getContracts().remove(contract);
				carrier.getExpiredContracts().add(contract);
			}

	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize,
			double startPickup, double endPickup, double startDelivery,
			double endDelivery) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getId() {
		// TODO Auto-generated method stub
		return null;
	}


	
	
}
