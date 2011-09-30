package playground.mzilske.freight.events;

import playground.mzilske.freight.Contract;

public class ShipperTSPContractAcceptEvent extends ContractAcceptEvent{
	
	public ShipperTSPContractAcceptEvent(Contract contract) {
		super(contract);
	}

}
