package demand.mutualReplanning;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;
import demand.demandObject.DemandPlanImpl;
import demand.offer.Offer;
import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class OfferReplanningStrategyModuleImpl extends OfferReplanningStrategyModule{

	
	public OfferReplanningStrategyModuleImpl(DemandObject demandObject) {
		super(demandObject);
	}
	
	public OfferReplanningStrategyModuleImpl() {
		super();
	}
	
	@Override
	public void handlePlan(DemandPlan demandPlan) {
		Collection<Offer> offers = recieveOffers(lsps);
		plan = createPlan(demandPlan, offers);
		demandObject.setSelectedPlan(plan);
	}
	
	protected Collection<Offer> recieveOffers(Collection<LSPDecorator> lsps){
		return demandObject.getOfferRequester().requestOffers(lsps);				
	}
	
	protected DemandPlan createPlan(DemandPlan demandPlan, Collection<Offer> offers) {
			return demandObject.getDemandPlanGenerator().createDemandPlan(offers);
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
			
	}
		
	
}
