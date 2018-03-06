package demandObjectTests;

import java.util.Collection;

import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;
import demand.demandObject.DemandPlanGenerator;
import demand.demandObject.DemandPlanImpl;
import demand.demandObject.ShipperShipmentImpl;
import demand.offer.Offer;

public class HalfLotSizeDemandPlanGenerator implements DemandPlanGenerator{

	private DemandObject demandObject;
	
	@Override
	public DemandPlan createDemandPlan(Collection<Offer> offers) {
		Offer singleOffer = offers.iterator().next();
		DemandPlanImpl.Builder planBuilder = DemandPlanImpl.Builder.newInstance();
		planBuilder.setLsp(singleOffer.getLsp());
		planBuilder.setLogisticsSolutionId(singleOffer.getSolution().getId());
		planBuilder.setDemandObject(demandObject);
		
		ShipperShipmentImpl.Builder shipmentBuilder = ShipperShipmentImpl.Builder.newInstance();
		shipmentBuilder.setDemandObject(demandObject);
		shipmentBuilder.setId(demandObject.getSelectedPlan().getShipment().getId());
		shipmentBuilder.setEndTimeWindow(demandObject.getSelectedPlan().getShipment().getEndTimeWindow());
		shipmentBuilder.setStartTimeWindow(demandObject.getSelectedPlan().getShipment().getStartTimeWindow());
		shipmentBuilder.setServiceTime(demandObject.getSelectedPlan().getShipment().getServiceTime());
		shipmentBuilder.setShipmentSize(demandObject.getSelectedPlan().getShipment().getShipmentSize()/2);
		planBuilder.setShipperShipment(shipmentBuilder.build());
		return planBuilder.build();
	}

	@Override
	public void setDemandObject(DemandObject demandObject) {
		this.demandObject = demandObject;
	}

}
