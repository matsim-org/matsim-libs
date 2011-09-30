package playground.mzilske.freight.events;

import playground.mzilske.freight.Contract;

public class TSPCarrierContractAcceptEvent extends ContractAcceptEvent{
	
	public TSPCarrierContractAcceptEvent(Contract contract) {
		super(contract);
	}
	
}
