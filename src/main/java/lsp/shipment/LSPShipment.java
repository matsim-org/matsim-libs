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

	public Id<LSPShipment> getId();
	
	public Id<Link> getFromLinkId();
	
	public Id<Link> getToLinkId();
	
	public TimeWindow getStartTimeWindow();
	
	public TimeWindow getEndTimeWindow();
	
	public ShipmentPlan getShipmentPlan();
	
	public ShipmentPlan getLog();
	
	public int getCapacityDemand();
	
	public double getServiceDuration();
	
	public Collection<EventHandler> getEventHandlers();
	
	public Collection<Requirement> getRequirements();
	
//	public Collection<UtilityFunction> getUtilityFunctions();
	
	public Collection<LSPInfo> getInfos();
	
	public Id<LogisticsSolution> getSolutionId();
	
//	public void setSolutionId(Id<LogisticsSolution> Id);
	
}
