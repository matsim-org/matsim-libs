package freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPKnowledge;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class TSPUtils {
	
	public static TransportServiceProviderImpl createTSP(String id){
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(makeId(id));
		tsp.setTspCapabilities(new TSPCapabilities());
		tsp.setKnowledge(new TSPKnowledge());
		return tsp;
	}
	
	public static void createAndAddTranshipmentCentre(TransportServiceProviderImpl tsp, String tccLocationId){
		if(tsp.getTspCapabilities() != null){
			tsp.getTspCapabilities().getTransshipmentCentres().add(makeId(tccLocationId));
		}
		else{
			TSPCapabilities caps = new TSPCapabilities();
			tsp.setTspCapabilities(caps);
			caps.getTransshipmentCentres().add(makeId(tccLocationId));
		}
	}
	
	public static TSPShipment createTSPShipment(Id from, Id to, int size, double startPickUp, double endPickUp, 
			double startDelivery, double endDelivery){
		TimeWindow pickUpTW = new TimeWindow(startPickUp,endPickUp);
		TimeWindow deliverTW = new TimeWindow(startDelivery, endDelivery);
		return new TSPShipment(from,to,size,pickUpTW,deliverTW);
	}
	
	public static TSPShipment createTSPShipment(Id from, Id to, int size, TimeWindow pickupTW, TimeWindow deliveryTW){
		return new TSPShipment(from,to,size,pickupTW,deliveryTW);
	}
	
	public static TSPContract createTSPContract(TSPShipment shipment){
		return new TSPContract(shipment, new TSPOffer());
	}
	
	public static void createAndAddTSPContract(TransportServiceProviderImpl tsp, TSPShipment shipment){
		tsp.getContracts().add(createTSPContract(shipment));
	}
	
	public static Collection<TSPShipment> splitTSPShipment(TSPShipment shipment, int maxShipmentSize){
		Collection<TSPShipment> shipments = new ArrayList<TSPShipment>();
		int nOfPallets = shipment.getSize();
		double quotient = (double)nOfPallets/(double)maxShipmentSize;
		if(quotient>1){
			int nOfShipmentsWithMaxVehicleCap = (int)Math.floor(quotient);
			int restOfShipment = nOfPallets % maxShipmentSize;
			for(int i=0;i<nOfShipmentsWithMaxVehicleCap;i++){
				shipments.add(createTSPShipment(shipment.getFrom(), shipment.getTo(), maxShipmentSize, shipment.getPickUpTimeWindow(), shipment.getDeliveryTimeWindow()));
			}
			if(restOfShipment > 0){
				shipments.add(createTSPShipment(shipment.getFrom(), shipment.getTo(), restOfShipment, shipment.getPickUpTimeWindow(), shipment.getDeliveryTimeWindow()));
			}
		}
		else{
			shipments.add(shipment);
		}
		return shipments;
		
		
		
	}
	
	private static Id makeId(String id) {
		return new IdImpl(id);
	}

	public static TSPContract createTSPContract(TSPShipment shipment, TSPOffer tspOffer) {
		TSPContract contract = new TSPContract(shipment, tspOffer);
		return contract;
	}

}
