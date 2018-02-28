package demand.offer;

import java.util.Collection;

import demand.decoratedLSP.LSPDecorator;

public interface OfferUpdater  {

	public void updateOffers();
	public Collection<OfferVisitor> getOfferVisitors();
	public void setLSP (LSPDecorator lsp);
}
