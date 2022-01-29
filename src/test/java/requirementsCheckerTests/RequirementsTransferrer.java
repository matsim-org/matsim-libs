package requirementsCheckerTests;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferTransferrer;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

public class RequirementsTransferrer implements OfferTransferrer{

	private LSPDecorator lsp;
	private final Collection<LogisticsSolutionDecorator> feasibleSolutions;
	
	public RequirementsTransferrer() {
		this.feasibleSolutions = new ArrayList<LogisticsSolutionDecorator>();
	}
	
	
	@Override
	public Offer transferOffer(DemandObject object, String type, Id<LogisticsSolution> solutionId) {
		feasibleSolutions.clear();		
		label:			
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
					LogisticsSolutionDecorator solutionWithOffers = (LogisticsSolutionDecorator) solution;
					for(Requirement requirement : object.getRequirements()) {
						if(requirement.checkRequirement(solutionWithOffers) == false) {
							continue label;
						}
					}
					feasibleSolutions.add(solutionWithOffers);
				}
				
			LogisticsSolutionDecorator chosenSolution = feasibleSolutions.iterator().next();
			return chosenSolution.getOffer(object, type);
		
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}


	@Override
	public LSPDecorator getLSP() {
		return lsp;
	}
}
