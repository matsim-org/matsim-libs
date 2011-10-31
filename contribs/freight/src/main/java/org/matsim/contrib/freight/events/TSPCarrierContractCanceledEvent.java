package org.matsim.contrib.freight.events;

import org.matsim.contrib.freight.carrier.Contract;

public class TSPCarrierContractCanceledEvent extends ContractCanceledEvent{

	public TSPCarrierContractCanceledEvent(Contract contract) {
		super(contract);
	}

}
