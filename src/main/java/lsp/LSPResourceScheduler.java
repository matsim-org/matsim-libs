package lsp;

import java.util.ArrayList;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentComparator;

/**
 * Resources are scheduled separately by calling their individual scheduling algorithm.
 *
 * Within this algorithm, some methods are abstract, whereas others
 * have a default implementation for forward scheduling.
 * The former ones are specified in a suitable way by the corresponding Resource whereas the latter are only specified in
 * the abstract parent class in order to coordinate the way in which the LSPShipments are
 * handed over between subsequent Resources.
 * The abstract methods deal with aspects that are specific to the Resource which
 * contains the implementation of the ResourceScheduler.
 *
 * Forwarding of LSPShipments is done by the two methods
 * presortIncomingShipments() and switchHandledShipments(int bufferTime).
 */
public abstract class LSPResourceScheduler {

	protected LSPResource resource;
	protected ArrayList<ShipmentWithTime>shipments;
	
	
	public final void scheduleShipments(LSPResource resource, int bufferTime) {
		this.resource = resource;
		this.shipments = new ArrayList<>();
		initializeValues(resource);
		presortIncomingShipments();	
		scheduleResource();
		updateShipments();
		switchHandeledShipments(bufferTime);
		shipments.clear();	
	}

	/**
	 * Is in charge of the initialization of the actual
	 * scheduling process for the concerned Resource. Depending on the concrete shape
	 * of this process, there are mainly values to be deleted that are still stored from the
	 * previous iteration or the infrastructure for the used algorithm has to be set up.
	 * @param resource
	 */
	protected abstract void initializeValues(LSPResource resource);

	/**
	 * Controls the actual scheduling process that depends on the shape
	 * and task of the Resource.
	 */
	protected abstract void scheduleResource();

	/**
	 * Endows the involved {@link LSPShipment}s with information that resulted
	 * from the scheduling in a narrow sense in scheduleResource(). The information
	 * can be divided into two main components.
	 * 1.) the schedule of the {@link LSPShipment}s is updated if necessary
	 * 2.) the information for a later logging of the is added.
	 */
	protected abstract void updateShipments();
		
	public final void presortIncomingShipments(){
		this.shipments = new ArrayList<>();
		for(LogisticsSolutionElement element : resource.getClientElements()){
			shipments.addAll(element.getIncomingShipments().getShipments());		
		}
		shipments.sort( new ShipmentComparator() );
	}
	
	
	public final void switchHandeledShipments(int bufferTime){
		for( ShipmentWithTime shipment : shipments) {
			double endOfTransportTime = shipment.getShipment().getShipmentPlan().getMostRecentEntry().getEndTime() + bufferTime;
			ShipmentWithTime outgoingTuple = new ShipmentWithTime(endOfTransportTime,shipment.getShipment());
			for(LogisticsSolutionElement element : resource.getClientElements()){
				if(element.getIncomingShipments().getShipments().contains(shipment)){
					element.getOutgoingShipments().getShipments().add(outgoingTuple);
					element.getIncomingShipments().getShipments().remove(shipment);
					if(element.getNextElement() != null) {
						element.getNextElement().getIncomingShipments().getShipments().add(outgoingTuple);
						element.getOutgoingShipments().getShipments().remove(outgoingTuple);
					}
				}
			}
		}	
	}	
	
	
}
