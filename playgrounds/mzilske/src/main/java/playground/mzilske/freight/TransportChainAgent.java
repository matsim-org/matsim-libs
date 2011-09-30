package playground.mzilske.freight;


public interface TransportChainAgent {

//	public abstract List<CarrierContract> createCarrierContracts();

	public abstract void informPickup(Shipment shipment, double time);

	public abstract void informDelivery(Shipment shipment, double time);

	public abstract int getNumberOfTranshipments();

	public abstract boolean hasSucceeded();

	public abstract double getFees();

	public abstract TransportChain getTransportChain();

	public abstract void reset();
	
	public boolean hasShipment(Shipment shipment);

//	public abstract Collection<Contract> getCarrierContracts();

//	public abstract Collection<CarrierShipment> getShipments();
	
	public abstract void addCarrierContract(Contract contract);

}