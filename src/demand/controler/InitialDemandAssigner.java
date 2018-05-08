package demand.controler;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPDecorators;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;
import demand.demandObject.DemandPlan;
import demand.offer.Offer;
import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class InitialDemandAssigner implements StartupListener{

	private DemandObjects demandObjects;
	private LSPDecorators lsps;
			
	public InitialDemandAssigner(DemandObjects demandObjects, LSPDecorators lsps) {
		this.demandObjects = demandObjects;
		this.lsps = lsps;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		for(DemandObject demandObject : demandObjects.getDemandObjects().values()) {
			if(demandObject.getSelectedPlan() == null) {
				createInitialPlan(demandObject);
			}
				assignShipmentToLSP(demandObject);
		}

		for(LSPDecorator lsp : lsps.getLSPs().values()) {
			lsp.scheduleSoultions();
		}
	}

	private void createInitialPlan(DemandObject demandObject) {
		Collection<Offer> offers = demandObject.getOfferRequester().requestOffers(lsps.getLSPs().values());
		DemandPlan initialPlan = demandObject.getDemandPlanGenerator().createDemandPlan(offers);
		demandObject.setSelectedPlan(initialPlan);	
	}
	
	private void assignShipmentToLSP(DemandObject demandObject) {
		Id<LSPShipment> id = Id.create(demandObject.getSelectedPlan().getShipment().getId(), LSPShipment.class);
		LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
		builder.setFromLinkId(demandObject.getFromLinkId());
		builder.setToLinkId(demandObject.getToLinkId());
		builder.setCapacityDemand((int)demandObject.getSelectedPlan().getShipment().getShipmentSize());
		builder.setServiceTime(demandObject.getSelectedPlan().getShipment().getServiceTime());
		builder.setStartTimeWindow(demandObject.getSelectedPlan().getShipment().getStartTimeWindow());
		builder.setEndTimeWindow(demandObject.getSelectedPlan().getShipment().getEndTimeWindow());
		for(Info info : demandObject.getInfos()) {
			builder.addInfo(info);
		}
		LSPShipment lspShipment = builder.build();
		demandObject.getSelectedPlan().getLsp().assignShipmentToSolution(lspShipment, demandObject.getSelectedPlan().getSolutionId());
		demandObject.getSelectedPlan().getShipment().setLSPShipment(lspShipment);
	}
	
	
}
