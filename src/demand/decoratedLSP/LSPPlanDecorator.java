package demand.decoratedLSP;

import demand.offer.OfferTransferrer;
import demand.offer.OfferUpdater;
import lsp.LSPPlan;

public interface LSPPlanDecorator extends LSPPlan{

	public void setOfferTransferrer(OfferTransferrer transferrer);
	public OfferTransferrer getOfferTransferrer();
	
	
}
