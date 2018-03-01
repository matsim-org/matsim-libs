package example.requirementsChecking;

import java.util.ArrayList;
import java.util.Collection;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import lsp.shipment.Requirement;

public class RequirementsAssigner implements  ShipmentAssigner {

	private LSP lsp;
	private Collection<LogisticsSolution> feasibleSolutions;
	
	public RequirementsAssigner() {
		this.feasibleSolutions = new ArrayList<LogisticsSolution>();
	}
	
	@Override
	public void assignShipment(LSPShipment shipment) {
		feasibleSolutions.clear();
		
		label:
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			for(Requirement requirement : shipment.getRequirements()) {
				if(requirement.checkRequirement(solution) == false) {
					
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

	@Override
	public LSP getLSP() {
		return lsp;
	}

}
