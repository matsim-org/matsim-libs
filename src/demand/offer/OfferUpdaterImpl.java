package demand.offer;

import java.util.ArrayList;
import java.util.Collection;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.decoratedLSP.LogisticsSolutionWithOffers;
import lsp.LogisticsSolution;

public class OfferUpdaterImpl implements OfferUpdater{

	private LSPDecorator  lsp;
	private Collection <OfferVisitor> visitors;
	
	public OfferUpdaterImpl() {
		this.visitors = new ArrayList<OfferVisitor>();
	}
	
	
	@Override
	public void updateOffers() {
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			if(solution instanceof LogisticsSolutionDecorator) {
				LogisticsSolutionDecorator offerSolution = (LogisticsSolutionDecorator) solution;
				for(OfferVisitor visitor : visitors) {
					if(visitor.getLogisticsSolution() == solution) {
						for(Offer offer : offerSolution.getOfferFactory().getOffers()) {
							if(offer.getClass() == visitor.getOfferClass()) {
								visitor.visit(offer);
							}
						}
					}
				}
			}
		}
		
	}

	@Override
	public Collection<OfferVisitor> getOfferVisitors() {
		return visitors;
	}

	public void setLSP(LSPDecorator  lsp) {
		this.lsp = lsp;
	}
}
