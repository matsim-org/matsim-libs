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
import playground.mzilske.freight.events.ShipperTSPContractAcceptEvent;
import playground.mzilske.freight.events.ShipperTSPContractAcceptEventHandler;
import playground.mzilske.freight.events.ShipperTSPContractCanceledEvent;
import playground.mzilske.freight.events.ShipperTSPContractCanceledEventHandler;
import playground.mzilske.freight.events.TSPShipmentDeliveredEvent;
import playground.mzilske.freight.events.TSPShipmentDeliveredEventHandler;
import playground.mzilske.freight.events.TSPShipmentPickUpEvent;
import playground.mzilske.freight.events.TSPShipmentPickUpEventHandler;
import freight.api.ShipperAgentFactory;

public class ShipperAgentTracker implements ShipperTSPContractAcceptEventHandler, ShipperTSPContractCanceledEventHandler, TSPShipmentPickUpEventHandler, TSPShipmentDeliveredEventHandler{
	
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

	private ShipperAgent findShipperAgent(Id shipperId) {
		for(ShipperAgent agent : shipperAgents){
			if(agent.getId().equals(shipperId)){
				return agent;
			}
		}
		return null;
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

	@Override
	public void reset(int iteration) {
		
		
	}

	@Override
	public void handleEvent(ShipperTSPContractAcceptEvent event) {
		findShipperAgent(event.getContract().getBuyer()).informTSPContractAccept(event.getContract());
	}

	@Override
	public void handleEvent(ShipperTSPContractCanceledEvent event) {
		findShipperAgent(event.getContract().getBuyer()).informTSPContractCanceled(event.getContract());
	}

	@Override
	public void handleEvent(TSPShipmentDeliveredEvent event) {
		
		
	}

	@Override
	public void handleEvent(TSPShipmentPickUpEvent event) {
		// TODO Auto-generated method stub
		
	}
}
