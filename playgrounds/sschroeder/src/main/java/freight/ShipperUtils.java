package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TimeWindow;

public class ShipperUtils {
	
	static ShipperAgent createShipperAgent(ShipperImpl shipper){
		return new ShipperAgentImpl(shipper);
	}

	public static ShipperImpl createShipper(String id, String locationId){
		return new ShipperImpl(makeId(id), makeId(locationId));
	}

	private static Id makeId(String id) {
		return new IdImpl(id);
	}
	
	public static ShipperShipment createShipment(String from, String to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery){
		TimeWindow pickTw = makeTW(startPickup, endPickup);
		TimeWindow deliveryTw = makeTW(startDelivery, endDelivery);
		return new ShipperShipment(makeId(from), makeId(to), size, pickTw, deliveryTw);
	}
	
	private static TimeWindow makeTW(double startDelivery, double endDelivery) {
		return new TimeWindow(startDelivery,endDelivery);
	}

	public static CommodityFlow createCommodityFlow(String from, String to, int size, double value) {
		return createCommodityFlow(makeId(from), makeId(to), size, value);
	}
	
	public static CommodityFlow createCommodityFlow(Id from, Id to, int size, double value){
		return new CommodityFlow(from, to, size, value);
	}
	
	public static ShipperContract createShipperContract(CommodityFlow commodityFlow){
		return new ShipperContract(commodityFlow);
	}

	public static ShipperPlan createPlan(Collection<ScheduledCommodityFlow> scheduledCommodityFlows) {
		return new ShipperPlan(scheduledCommodityFlows);
	}

	public static ScheduledCommodityFlow createScheduledCommodityFlow(CommodityFlow commodityFlow, Collection<ShipperShipment> shipments, TSPOffer offer) {
		return new ScheduledCommodityFlow(commodityFlow, shipments, offer);
	}

	public static ShipperShipment createShipment(TSPShipment tspShipment) {
		ShipperShipment shipment = createShipment(tspShipment.getFrom().toString(), tspShipment.getTo().toString(), tspShipment.getSize(), 
				tspShipment.getPickupTimeWindow().getStart(), tspShipment.getPickupTimeWindow().getEnd(), tspShipment.getDeliveryTimeWindow().getStart(),
				tspShipment.getDeliveryTimeWindow().getEnd());
		return shipment;
	}

	public static ShipperShipment createShipment(Id from, Id to, int size, double pickupStart, double pickupEnd, double deliveryStart, double deliveryEnd) {
		return new ShipperShipment(from,to,size,makeTW(pickupStart,pickupEnd),makeTW(deliveryStart,deliveryEnd));
	}

	public static void createAndAddContract(ShipperImpl shipper, CommodityFlow commodityFlow) {
		shipper.getContracts().add(createShipperContract(commodityFlow));
	}

	public static ScheduledCommodityFlow createScheduledCommodityFlow(Id id,CommodityFlow currentComFlow,List<ShipperShipment> currentShipments, TSPOffer currentTspOffer) {
		Collection<Contract> contracts = new ArrayList<Contract>();
		for(ShipperShipment s : currentShipments){
			TSPShipment tspShipment = createTSPShipment(s);
			Contract contract = new TSPContract(id,currentTspOffer.getId(),tspShipment,currentTspOffer);
			contracts.add(contract);
		}
		return new ScheduledCommodityFlow(currentComFlow, currentShipments, contracts);
	}

	private static TSPShipment createTSPShipment(ShipperShipment s) {
		return TSPUtils.createTSPShipment(s.getFrom(), s.getTo(), s.getSize(), s.getPickupTimeWindow(), s.getDeliveryTimeWindow());
	}
	
	
}
