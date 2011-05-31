package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import playground.mzilske.freight.TSPContract;

public class ShipperAgentTracker {
	
	private static Logger logger = Logger.getLogger(ShipperAgentTracker.class);
	
	private Collection<ShipperImpl> shippers;
	
	private Collection<ShipperAgent> shipperAgents = new ArrayList<ShipperAgent>();

	public ShipperAgentTracker(Collection<ShipperImpl> shippers) {
		super();
		this.shippers = shippers;
		createShipperAgents();
	}

	private void createShipperAgents() {
		for(ShipperImpl s : shippers){
			ShipperAgent agent = ShipperUtils.createShipperAgent(s);
			shipperAgents.add(agent);
		}
	}
	
	public Collection<TSPContract> createTSPContracts(){
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		for(ShipperAgent agent : shipperAgents){
			List<TSPContract> agentContracts = agent.createTSPContracts();
			contracts.addAll(agentContracts);
		}
		return contracts;
	}
}
