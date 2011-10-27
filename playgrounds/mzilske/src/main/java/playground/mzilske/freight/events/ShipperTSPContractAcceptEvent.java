package playground.mzilske.freight.events;

import org.matsim.contrib.freight.api.Contract;

public class ShipperTSPContractAcceptEvent extends ContractAcceptEvent{
	
	public ShipperTSPContractAcceptEvent(Contract contract) {
		super(contract);
	}

}
