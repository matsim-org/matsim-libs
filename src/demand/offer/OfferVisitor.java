package demand.offer;

import lsp.LogisticsSolution;

public interface OfferVisitor {

	public void visit(Offer offer);
	public Class<? extends Offer> getOfferClass();
	public LogisticsSolution getLogisticsSolution();
}
