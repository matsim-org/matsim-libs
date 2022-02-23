package lsp;

import java.util.Collection;

import lsp.shipment.LSPShipment;

/**
 * Each LogisticsSolutionElement maintains two collections of WaitingShipments.
 * Instances of the latter class contain tuples of LSPShipments and time stamps.
 *
 * The first of these collections stores LSPShipments that are waiting for their treatment in this element or more precisely the Resource that is in
 * charge of the actual physical handling.
 *
 * The second one stores shipments that have already been treated.
 *
 * At the beginning of the scheduling process, all LSPShipments are added
 * to the collection of incoming shipments of the first LogisticsSolutionElement of the
 * LogisticsSolution to which they were assigned before. The tuples in the collection of
 * WaitingShipments thus consist of the shipments themselves and a time stamp that states
 * when they arrived there (see 3.9). In the case of the first LogisticsSolutionElement,
 * this time stamp corresponds to the start time window of the LSPShipment
 */
public interface WaitingShipments {

	void addShipment(double time, LSPShipment shipment);
	
	Collection<ShipmentWithTime> getSortedShipments();
	
	Collection<ShipmentWithTime> getShipments();
	
	void clear();
	
}
