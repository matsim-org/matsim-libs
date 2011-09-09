package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.mzilske.freight.TSPContract;
import freight.api.ShipperAgentFactory;
import freight.listener.ShipperDetailedCostStatusHandler;
import freight.listener.ShipperTotalCostStatusHandler;

public class ShipperAgentTracker {
	
	private static Logger logger = Logger.getLogger(ShipperAgentTracker.class);
	
	private Collection<ShipperImpl> shippers;
	
	private Collection<ShipperAgent> shipperAgents = new ArrayList<ShipperAgent>();
	
	private ShipperAgentFactory shipperAgentFactory;
	
	private EventsManager eventsManager;

	public ShipperAgentTracker(Collection<ShipperImpl> shippers, ShipperAgentFactory shipperAgentFactory) {
		super();
		this.shipperAgentFactory = shipperAgentFactory;
		this.shippers = shippers;
		createShipperAgents();
		eventsManager = EventsUtils.createEventsManager();
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
	
	public EventsManager getEventsManager() {
		return eventsManager;
	}
	
	public void processEvent(Event event){
		eventsManager.processEvent(event);
	}

	public void scorePlans(){
		for(ShipperImpl s : shippers){
			findShipperAgent(s.getId()).scoreSelectedPlan();
		}
	}
}
