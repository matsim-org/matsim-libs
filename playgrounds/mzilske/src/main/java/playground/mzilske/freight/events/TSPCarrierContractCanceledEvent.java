package playground.mzilske.freight.events;

import org.matsim.contrib.freight.api.Contract;

public class TSPCarrierContractCanceledEvent extends ContractCanceledEvent{

	public TSPCarrierContractCanceledEvent(Contract contract) {
		super(contract);
	}

}
