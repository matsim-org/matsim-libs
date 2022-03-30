package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import lsp.controler.LSPSimulationTracker;


public interface LogisticsSolutionElement {

	Id<LogisticsSolutionElement> getId();
	
	void setLogisticsSolution(LogisticsSolution solution);
	
	LogisticsSolution getLogisticsSolution();

	void connectWithNextElement(LogisticsSolutionElement element);
	
	LSPResource getResource();
	
	LogisticsSolutionElement getPreviousElement();
	
	LogisticsSolutionElement getNextElement();

	/**
	 * This collections stores LSPShipments that are waiting for their treatment in this element or more precisely the Resource that is in
	 *  charge of the actual physical handling.
	 *
	 * @return WaitingShipments
	 */
	WaitingShipments getIncomingShipments();

	/**
	 *  Shipments that have already been treated.
	 */
	WaitingShipments getOutgoingShipments();

	void addSimulationTracker(LSPSimulationTracker tracker);
    
    Collection<LSPInfo> getInfos();
    
    Collection<EventHandler> getEventHandlers();
    
    Collection <LSPSimulationTracker> getSimulationTrackers();

}
