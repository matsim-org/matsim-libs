package lsp.shipment;

import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

public interface AbstractShipmentPlanElement {
	
	public LogisticsSolutionElement getSolutionElement();
	
	public Id<Resource> getResourceId();
	
	public String getElementType();
	
	public double getStartTime();
	
	public double getEndTime();
	
}
