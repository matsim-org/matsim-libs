package lsp.shipment;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.events.handler.EventHandler;

import demand.UtilityFunction;
import lsp.LogisticsSolution;
import lsp.functions.LSPInfo;

public interface LSPShipment {

	Id<LSPShipment> getId(); // same as in CarrierShipment
	
	Id<Link> getFrom(); // same as in CarrierShipment
	
	Id<Link> getTo(); // same as in CarrierShipment
	
	TimeWindow getPickupTimeWindow(); // same as in CarrierShipment
	
	TimeWindow getDeliveryTimeWindow(); // same as in CarrierShipment

	int getSize(); // same as in CarrierShipment

	double getDeliveryServiceTime(); // same as in CarrierShipment

	double getPickupServiceTime(); // same as in CarrierShipment

	ShipmentPlan getShipmentPlan();
	
	ShipmentPlan getLog();
	
	Collection<EventHandler> getEventHandlers();
	
	Collection<Requirement> getRequirements();
	
//	Collection<UtilityFunction> getUtilityFunctions();
	
	Collection<LSPInfo> getInfos();
	
	Id<LogisticsSolution> getSolutionId();
	
//	void setSolutionId(Id<LogisticsSolution> Id);
	
}
