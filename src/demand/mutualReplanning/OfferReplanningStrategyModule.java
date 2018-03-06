package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

import demand.decoratedLSP.LSPDecorator;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;
import demand.demandObject.DemandPlanImpl;
import demand.offer.Offer;
import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public abstract class OfferReplanningStrategyModule implements GenericPlanStrategyModule<DemandPlan>{

	protected DemandObject demandObject;
	protected Collection<LSPDecorator> lsps;
	protected DemandPlan plan; 
	
	
	public OfferReplanningStrategyModule() {
		
	}
	
	public OfferReplanningStrategyModule(DemandObject demandObject) {
		this.demandObject = demandObject;
	}
		
	@Override
	public void handlePlan(DemandPlan demandPlan) {
		Collection<Offer> offers = recieveOffers(lsps);
		plan = createPlan(demandPlan, offers);
		demandObject.setSelectedPlan(plan);
	}
	
	protected abstract Collection<Offer> recieveOffers(Collection<LSPDecorator> lsps);
	protected abstract DemandPlan createPlan(DemandPlan demandPlan, Collection<Offer> offers);
			
	@Override
	public void finishReplanning() {
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

	public void setLSPS(Collection<LSPDecorator> lsps) {
		this.lsps = lsps;
	}
	
	public void setDemandObject (DemandObject demandObject) {
		this.demandObject = demandObject;
	}
	
	public DemandObject getDemandObject () {
		return demandObject;
	}
}
