package lsp.shipment;

import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public interface ShipmentPlanElement {
	
	LogisticsSolutionElement getSolutionElement();
	
	Id<LSPResource> getResourceId();
	
	String getElementType();
	
	double getStartTime();
	
	double getEndTime();
	
}
