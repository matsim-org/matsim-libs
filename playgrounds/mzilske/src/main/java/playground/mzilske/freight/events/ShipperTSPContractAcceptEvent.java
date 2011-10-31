package playground.mzilske.freight.events;

import org.matsim.contrib.freight.carrier.Contract;
import org.matsim.contrib.freight.events.ContractAcceptEvent;

public class ShipperTSPContractAcceptEvent extends ContractAcceptEvent {
	
	public ShipperTSPContractAcceptEvent(Contract contract) {
		super(contract);
	}

}
