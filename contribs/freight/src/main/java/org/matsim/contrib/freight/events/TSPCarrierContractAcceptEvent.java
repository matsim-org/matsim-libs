package org.matsim.contrib.freight.events;

import org.matsim.contrib.freight.carrier.Contract;

public class TSPCarrierContractAcceptEvent extends ContractAcceptEvent{
	
	public TSPCarrierContractAcceptEvent(Contract contract) {
		super(contract);
	}
	
}
