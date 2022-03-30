package example.lspAndDemand.requirementsChecking;

import java.util.ArrayList;
import java.util.Collection;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import lsp.shipment.Requirement;

class RequirementsAssigner implements  ShipmentAssigner {

	private LSP lsp;
	private final Collection<LogisticsSolution> feasibleSolutions;
	
	public RequirementsAssigner() {
		this.feasibleSolutions = new ArrayList<>();
	}
	
	@Override
	public void assignToSolution(LSPShipment shipment) {
		feasibleSolutions.clear();
		
		label:
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			for(Requirement requirement : shipment.getRequirements()) {
				if(!requirement.checkRequirement(solution)) {
					
					continue label;
				}
			}
			feasibleSolutions.add(solution);
		}
		LogisticsSolution chosenSolution = feasibleSolutions.iterator().next();
		chosenSolution.assignShipment(shipment);
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

}
