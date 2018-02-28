package demand.offer;

import lsp.LSP;
import lsp.LogisticsSolution;

public class DefaultOfferImpl implements Offer {

	private LSP lsp;
	private LogisticsSolution solution;
	
	public DefaultOfferImpl(LSP lsp, LogisticsSolution logisticsSolution) {
		this.lsp = lsp;
		this.solution = logisticsSolution;
	}
	
	@Override
	public LSP getLsp() {
		return lsp;
	}

	@Override
	public LogisticsSolution getSolution() {
		return solution;
	}

	@Override
	public String getType() {
		return "DEFAULT";
	}

	public void update() {
		
	}

	@Override
	public void accept(OfferVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public void setSolution(LogisticsSolution solution) {
		this.solution = solution;
	}
	
	
}
