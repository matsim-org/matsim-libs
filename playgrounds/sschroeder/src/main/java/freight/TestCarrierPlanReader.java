package freight;

import java.util.ArrayList;
import java.util.Collection;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour.TourElement;

public class TestCarrierPlanReader {
	
	public static void main(String[] args) {
		Collection<CarrierImpl> carriers = new ArrayList<CarrierImpl>();
		CarrierPlanReader planReader = new CarrierPlanReader(carriers);
		planReader.read("output/sandBoxPlans.xml");
		for(CarrierImpl c : carriers){
			System.out.println(c.getId());
			for(Contract con : c.getContracts()){
				System.out.println(con.getShipment());
			}
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
				System.out.println("id=" + v.getVehicleId() + " linkId=" + v.getLocation());
			}
			for(ScheduledTour sT : c.getSelectedPlan().getScheduledTours()){
				System.out.println("vehicle=" + sT.getVehicle());
				for(Shipment s : sT.getTour().getShipments()){
					System.out.println(s);
				}
				System.out.println("start at " + sT.getTour().getStartLinkId());
				for(TourElement e : sT.getTour().getTourElements()){
					System.out.println(e.getActivityType() + " shipment=" + e.getShipment());
				}
				System.out.println("end at " + sT.getTour().getEndLinkId());
			}
			
		}
	}

}
