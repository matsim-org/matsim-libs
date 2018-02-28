package demand.decoratedLSP;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.ReplanningEvent;

import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferTransferrer;
import demand.offer.OfferUpdater;
import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.shipment.LSPShipment;

public interface LSPDecorator extends LSP {

	public Offer getOffer(DemandObject object, String type, Id<LogisticsSolution> solutionId);
	public void assignShipmentToSolution(LSPShipment shipment, Id<LogisticsSolution> id);
	public OfferUpdater getOfferUpdater();
	public void setOfferUpdater(OfferUpdater offerUpdater);
}
