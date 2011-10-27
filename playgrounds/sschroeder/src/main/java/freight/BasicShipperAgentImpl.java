package freight;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.api.Contract;
import playground.mzilske.freight.TSPContract;

import java.util.ArrayList;
import java.util.List;

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
