package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TSPContract;
import freight.ShipperAgent.DetailedCost;
import freight.ShipperAgent.TotalCost;
import freight.api.ShipperAgentFactory;
import freight.listener.ShipperDetailedCostListener;
import freight.listener.ShipperTotalCostListener;

public class ShipperAgentTracker {
	
	private static Logger logger = Logger.getLogger(ShipperAgentTracker.class);
	
	private Collection<ShipperImpl> shippers;
	
	private Collection<ShipperAgent> shipperAgents = new ArrayList<ShipperAgent>();
	
	private ShipperAgentFactory shipperAgentFactory;

	private Collection<ShipperTotalCostListener> totalCostListeners = new ArrayList<ShipperTotalCostListener>();

	private Collection<ShipperDetailedCostListener> detailedCostListeners = new ArrayList<ShipperDetailedCostListener>();

	public Collection<ShipperDetailedCostListener> getDetailedCostListeners() {
		return detailedCostListeners;
	}

	public Collection<ShipperTotalCostListener> getTotalCostListeners() {
		return totalCostListeners;
	}

	public ShipperAgentTracker(Collection<ShipperImpl> shippers, ShipperAgentFactory shipperAgentFactory) {
		super();
		this.shipperAgentFactory = shipperAgentFactory;
		this.shippers = shippers;
		createShipperAgents();
	}

	private void createShipperAgents() {
		for(ShipperImpl s : shippers){
			ShipperAgent agent = shipperAgentFactory.createShipperAgent(this, s);
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

	public Collection<TSPContract> removeScheduledComFlowAndGetAffectedTspContracts(Id shipperId, ScheduledCommodityFlow flow) {
		ShipperAgent agent = findShipperAgent(shipperId);
		Collection<TSPContract> tspContracts = null;
		if(agent != null){
			if(agent.hasCommodityFlow(flow)){
				tspContracts = agent.removeScheduledComFlowAndGetAffectedTspContracts(flow);
			}
			else{
				logger.warn("cannot remove comFlow");
			}
		}
		else{
			throw new IllegalStateException("shipper " + shipperId + " does not exist");
		}
		return tspContracts;
	}

	private ShipperAgent findShipperAgent(Id shipperId) {
		for(ShipperAgent agent : shipperAgents){
			if(agent.getId().equals(shipperId)){
				return agent;
			}
		}
		return null;
	}

	public Collection<TSPContract> registerScheduledComFlowAndGetAffectedTspContracts(Id shipperId, ScheduledCommodityFlow flow) {
		ShipperAgent agent = findShipperAgent(shipperId);
		Collection<TSPContract> tspContracts = null;
		if(agent != null){
			tspContracts = agent.registerScheduledComFlowAndGetAffectedTspContracts(flow);
		}
		else{
			throw new IllegalStateException("shipper " + shipperId + " does not exist");
		}
		return tspContracts;
	}

	public ShipperAgent getShipperAgent(Id id) {
		return findShipperAgent(id);
	}

	public void informTotalCost(TotalCost totalCost) {
		for(ShipperTotalCostListener l : totalCostListeners ){
			l.inform(totalCost);
		}
		
	}

	public void informDetailedCost(DetailedCost detailedCost) {
		for(ShipperDetailedCostListener l : detailedCostListeners ){
			l.inform(detailedCost);
		}
		
	}
	
	public void scorePlans(){
		for(ShipperImpl s : shippers){
			findShipperAgent(s.getId()).scoreSelectedPlan();
		}
	}
}
