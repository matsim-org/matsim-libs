package demand.offer;

import lsp.LSP;
import lsp.LogisticsSolution;

public interface Offer {

	public LSP getLsp();
	public LogisticsSolution getSolution();
	public String getType();
	public void accept(OfferVisitor visitor);
	public void update();
	public void setLSP (LSP lsp);
	public void setSolution(LogisticsSolution solution);
}
