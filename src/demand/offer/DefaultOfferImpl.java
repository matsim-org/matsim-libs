package demand.offer;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import lsp.LSP;
import lsp.LogisticsSolution;

public class DefaultOfferImpl implements Offer {

	private LSPDecorator lsp;
	private LogisticsSolutionDecorator solution;
	
	public DefaultOfferImpl(LSPDecorator lsp, LogisticsSolutionDecorator logisticsSolution) {
		this.lsp = lsp;
		this.solution = logisticsSolution;
	}
	
	@Override
	public LSPDecorator getLsp() {
		return lsp;
	}

	@Override
	public LogisticsSolutionDecorator getSolution() {
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
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}

	@Override
	public void setSolution(LogisticsSolutionDecorator solution) {
		this.solution = solution;
	}
	
	
}
