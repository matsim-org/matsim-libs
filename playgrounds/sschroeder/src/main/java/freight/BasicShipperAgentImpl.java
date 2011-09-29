package freight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPContract;

public abstract class BasicShipperAgentImpl implements ShipperAgent{
	
	private ShipperImpl shipper;
	
	private Id id;
	
	public BasicShipperAgentImpl(ShipperImpl shipper) {
		super();
		this.shipper = shipper;
		this.id = shipper.getId();
	}

	public Id getId(){
		return id;
	}

	/* (non-Javadoc)
	 * @see freight.ShipperAgent#createTSPContracts()
	 */
	@Override
	public List<TSPContract> createTSPContracts() {
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
			for(Contract c : sComFlow.getContracts()){
				contracts.add((TSPContract)c);
			}
		}
		return contracts;
	}
	
}
