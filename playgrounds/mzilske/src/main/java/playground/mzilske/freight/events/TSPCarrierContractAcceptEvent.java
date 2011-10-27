package playground.mzilske.freight.events;

import org.matsim.contrib.freight.api.Contract;

public class TSPCarrierContractAcceptEvent extends ContractAcceptEvent{
	
	public TSPCarrierContractAcceptEvent(Contract contract) {
		super(contract);
	}
	
}
