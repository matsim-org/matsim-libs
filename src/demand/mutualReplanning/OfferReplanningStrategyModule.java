package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

import demand.decoratedLSP.LSPWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;
import demand.offer.Offer;
import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public abstract class OfferReplanningStrategyModule implements GenericPlanStrategyModule<DemandPlan>{

	protected DemandObject demandObject;
	protected Collection<LSPWithOffers> lsps;
	
	public OfferReplanningStrategyModule(DemandObject demandObject, Collection<LSPWithOffers> lsps) {
		this.demandObject = demandObject;
		this.lsps = lsps;
	}
	
	
	@Override
	public void handlePlan(DemandPlan demandPlan) {
		Collection<Offer> offers = recieveOffers(lsps);
		DemandPlan plan = createPlan(offers);
		assignShipmentToLSP(plan);
	}
	
	protected abstract Collection<Offer> recieveOffers(Collection<LSPWithOffers> lsps);
	protected abstract DemandPlan createPlan(Collection<Offer> offers);
	
	private void assignShipmentToLSP(DemandPlan plan) {
		Id<LSPShipment> id = Id.create(plan.getShipment().getId(), LSPShipment.class);
		LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
		builder.setFromLinkId(demandObject.getFromLinkId());
		builder.setToLinkId(demandObject.getToLinkId());
		builder.setCapacityDemand((int)plan.getShipment().getShipmentSize());
		builder.setServiceTime(plan.getShipment().getServiceTime());
		builder.setStartTimeWindow(plan.getShipment().getStartTimeWindow());
		builder.setEndTimeWindow(plan.getShipment().getEndTimeWindow());
		for(Info info : demandObject.getInfos()) {
			builder.addInfo(info);
		}
		LSPShipment lspShipment = builder.build();
		plan.getLsp().assignShipmentToSolution(lspShipment, plan.getSolutionId());
		plan.getShipment().setLSPShipment(lspShipment);
		
	}
	
}
