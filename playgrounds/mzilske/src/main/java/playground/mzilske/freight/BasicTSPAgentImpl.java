package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TransportChain.ChainLeg;

public abstract class BasicTSPAgentImpl implements TSPAgent{
	
	private Logger logger = Logger.getLogger(BasicTSPAgentImpl.class);
	
	protected TransportServiceProvider tsp;
	
	protected Id id;
	
	protected Map<TransportChain,TransportChainAgent> chainAgentMap = new HashMap<TransportChain, TransportChainAgent>();

	protected Map<CarrierShipment,TransportChainAgent> shipmentChainAgentMap = new HashMap<CarrierShipment, TransportChainAgent>();
	
	protected Map<TSPOffer,List<CarrierOffer>> tspCarrierOfferMap = new HashMap<TSPOffer, List<CarrierOffer>>();
	
	private TransportChainAgentFactory chainAgentFactory;

	public BasicTSPAgentImpl(TransportServiceProvider tsp, TransportChainAgentFactory chainAgentFactory){
		this.tsp = tsp;
		this.id = tsp.getId();
		this.chainAgentFactory = chainAgentFactory;
	}
	
	public Id getId(){
		return this.id;
	}
	
	Map<CarrierShipment, TransportChainAgent> getShipmentChainMap() {
		return shipmentChainAgentMap;
	}

	public List<CarrierContract> getCarrierContracts(){
		clear();
		if(tsp.getSelectedPlan() == null){
			return Collections.EMPTY_LIST;
		}
		if(tsp.getSelectedPlan().getChains() == null){
			return Collections.EMPTY_LIST;
		}
		List<CarrierContract> carrierContracts = new ArrayList<CarrierContract>();
		for(TransportChain chain : tsp.getSelectedPlan().getChains()){
			TransportChainAgent chainAgent = chainAgentFactory.createChainAgent(chain);
			chainAgentMap.put(chain, chainAgent);
			for(ChainLeg leg : chain.getLegs()){
				carrierContracts.add(leg.getContract()); 
				CarrierShipment shipment = (CarrierShipment)leg.getContract().getShipment();
				chainAgent.addCarrierContract(leg.getContract());
				shipmentChainAgentMap.put(shipment, chainAgent);
			}
		}
		return carrierContracts;
	}

	private void clear() {
		chainAgentMap.clear();
		shipmentChainAgentMap.clear();
	}

	protected TransportChainAgent findChainAgent(TransportChain chain) {
		return chainAgentMap.get(chain);
	}

	@Override
	public void shipmentPickedUp(Shipment shipment, double time) {
		shipmentChainAgentMap.get(shipment).informPickup(shipment, time);
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TSPAgent#shipmentDelivered(playground.mzilske.freight.Shipment, double)
	 */
	@Override
	public void shipmentDelivered(Shipment shipment, double time) {
		shipmentChainAgentMap.get(shipment).informDelivery(shipment, time);
	}

	@Override
	public boolean hasShipment(Shipment shipment) {
		if(shipmentChainAgentMap.containsKey(shipment)){
			return true;
		}
		return false;
	}
	
	public List<CarrierOffer> getCarrierOffers(TSPOffer tspOffer){
		return tspCarrierOfferMap.get(tspOffer);
	}

	@Override
	public void informChainRemoved(TransportChain chain) {
		logger.debug("my plan changed (removal of a chain). me: " + tsp.getId());
		removeOldChain(chain);
	}
	
	private void removeOldChain(TransportChain oldChain) {
		for(ChainLeg leg : oldChain.getLegs()){ 
			CarrierShipment shipment = (CarrierShipment)leg.getContract().getShipment();
			shipmentChainAgentMap.remove(shipment);
		}
		chainAgentMap.remove(oldChain);
	}

	private void registerNewChain(TransportChain newChain) {
		TransportChainAgent chainAgent = chainAgentFactory.createChainAgent(newChain);
		chainAgentMap.put(newChain, chainAgent);
		for(ChainLeg leg : newChain.getLegs()){ 
			CarrierShipment shipment = (CarrierShipment)leg.getContract().getShipment();
			chainAgent.addCarrierContract(leg.getContract());
			shipmentChainAgentMap.put(shipment, chainAgent);
		}
	}

	@Override
	public void informChainAdded(TransportChain chain) {
		logger.info("my plan changed (additional chain). me: " + tsp.getId());
		registerNewChain(chain);
	}

	@Override
	public void informShipperContractAccepted(Contract contract) {
		logger.info("ahhh. got a new shipper contract. me: " + getId() + "; shipper " + contract.getBuyer());
		boolean added = tsp.getNewContracts().add((TSPContract)contract);
		if(!added){
			throw new IllegalStateException("could not add contract.");
		}
		tsp.getContracts().add((TSPContract)contract);
	}

	@Override
	public void informShipperContractCanceled(Contract contract) {
		logger.info("oh fuck. lost a contract. me: " + getId() + "; shipper " + contract.getBuyer());
		boolean removed = tsp.getContracts().remove(contract);
		if(!removed){
			throw new IllegalStateException("could not remove contract.");
		}
		tsp.getExpiredContracts().add((TSPContract)contract);
	}

}
