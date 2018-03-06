package demand.decoratedLSP;

import java.util.Collection;

import demand.offer.OfferTransferrer;
import demand.offer.OfferUpdater;
import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;

public interface LSPPlanDecorator extends LSPPlan{

	public void setOfferTransferrer(OfferTransferrer transferrer);
	public OfferTransferrer getOfferTransferrer();
	public Collection<LogisticsSolutionDecorator> getSolutionDecorators();
	public void addSolution (LogisticsSolutionDecorator solution);
	public void setLSP(LSPDecorator lsp);
	public LSPDecorator getLsp();
}
