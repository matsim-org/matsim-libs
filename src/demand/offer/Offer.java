package demand.offer;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import lsp.LSP;
import lsp.LogisticsSolution;

public interface Offer {

	public LSPDecorator getLsp();
	public LogisticsSolutionDecorator getSolution();
	public String getType();
	public void accept(OfferVisitor visitor);
	public void update();
	public void setLSP (LSPDecorator lsp);
	public void setSolution(LogisticsSolutionDecorator solution);
}
