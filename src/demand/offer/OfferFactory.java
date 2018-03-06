package demand.offer;

import java.util.Collection;

import demand.decoratedLSP.LogisticsSolutionWithOffers;
import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPWithOffers;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.demandObject.DemandObject;
import lsp.LSP;
import lsp.LogisticsSolution;

public interface OfferFactory {

	public Offer makeOffer(DemandObject object, String offerType);
	public Collection<Offer> getOffers();
	public LSPDecorator getLSP();
	public LogisticsSolutionDecorator getLogisticsSolution();
	public void setLogisticsSolution(LogisticsSolutionDecorator solution);
	public void setLSP(LSPDecorator lsp);
	public void addOffer(Offer offer);
}
