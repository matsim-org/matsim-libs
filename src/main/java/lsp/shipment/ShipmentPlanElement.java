package lsp.shipment;

import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

public interface ShipmentPlanElement {
	
	public LogisticsSolutionElement getSolutionElement();
	
	public Id<LSPResource> getResourceId();
	
	public String getElementType();
	
	public double getStartTime();
	
	public double getEndTime();
	
}
