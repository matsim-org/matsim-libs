package testMutualReplanning;

import demand.offer.Offer;
import demand.offer.OfferVisitor;
import lsp.LSP;
import lsp.LogisticsSolution;

public class NonsenseOffer implements Offer{

	private LSP lsp;
	private LogisticsSolution solution;
	
	
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
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
		
	}

	@Override
	public void setSolution(LogisticsSolution solution) {
		this.solution = solution;
		
	}

}
