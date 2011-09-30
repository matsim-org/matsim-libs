package playground.mzilske.freight.events;

import playground.mzilske.freight.Contract;

public class TSPCarrierContractCanceledEvent extends ContractCanceledEvent{

	public TSPCarrierContractCanceledEvent(Contract contract) {
		super(contract);
	}

}
