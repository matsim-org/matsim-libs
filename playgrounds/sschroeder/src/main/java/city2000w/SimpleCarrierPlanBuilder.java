package city2000w;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.TourBuilder;

public class SimpleCarrierPlanBuilder {

	public CarrierPlan buildPlan(CarrierImpl carrier) {
		boolean firstVehicle = true;
		List<ScheduledTour> sTours = new ArrayList<ScheduledTour>();
		for(CarrierVehicle cV : carrier.getCarrierCapabilities().getCarrierVehicles()){
			if(firstVehicle){
				TourBuilder tourBuilder = new TourBuilder();
				tourBuilder.scheduleStart(cV.getLocation());
				boolean firstContract = true;
				for(Contract c : carrier.getContracts()){
					if(firstContract){
						firstContract = false;
					}
					else{
						tourBuilder.scheduleGeneralActivity("pause", makeId("i(3,7)"), 600.0);
					}
					tourBuilder.schedulePickup(c.getShipment());
					tourBuilder.scheduleDelivery(c.getShipment());
				}
				tourBuilder.scheduleEnd(cV.getLocation());
				sTours.add(new ScheduledTour(tourBuilder.build(), cV, 0.0));
			}
		}
		return new CarrierPlan(sTours);
	}

	private double pickRandomStartTime() {
		return MatsimRandom.getRandom().nextDouble()*1800;
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}
}
