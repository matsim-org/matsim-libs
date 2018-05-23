package requirementsCheckerTests;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.offer.Offer;
import demand.offer.OfferVisitor;
import lsp.LSP;
import lsp.LogisticsSolution;

public class NonsenseOffer implements Offer{

	private LSPDecorator lsp;
	private LogisticsSolutionDecorator solution;
	
	
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
		return "nonsense";
	}

	@Override
	public void accept(OfferVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
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
