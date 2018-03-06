package testMutualreplanningWithOfferUpdate;

import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.offer.Offer;
import demand.offer.OfferVisitor;
import lsp.LogisticsSolution;

public class LinearOfferVisitor implements OfferVisitor {

	private LogisticsSolutionDecorator solution;
	
	public LinearOfferVisitor(LogisticsSolutionDecorator solution) {
		this.solution = solution;
	}
	
	
	@Override
	public void visit(Offer offer) {
		if(offer instanceof LinearOffer) {
			LinearOffer linearOffer = (LinearOffer) offer;
			linearOffer.update();
		}		
	}

	@Override
	public Class<? extends Offer> getOfferClass(){
		return LinearOffer.class;
	}

	@Override
	public LogisticsSolution getLogisticsSolution() {
		return solution;
	}

}
