package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;

public class MemorizeSelectedPlan implements CarrierPlanStrategyModule{
	
	@Override
	public void handleActor(Carrier carrier) {
		CarrierPlan copiedPlan = copyPlan(carrier.getSelectedPlan());
		addPlan(carrier,copiedPlan);
	}

	private void addPlan(Carrier carrier, CarrierPlan copiedPlan) {
		int memSize = 5;
		if(carrier.getPlans().size() < memSize){
			carrier.getPlans().add(copiedPlan);
		}
		else{
			boolean toBeInserted = false;
			CarrierPlan worstPlan = null;
			for(CarrierPlan p : carrier.getPlans()){
				if(copiedPlan.getScore() > p.getScore()){
					toBeInserted = true;
				}
				if(worstPlan == null){
					worstPlan = p;
				}
				else{
					if(p.getScore() < worstPlan.getScore()){
						worstPlan = p;
					}
				}
			}
			if(toBeInserted){
				carrier.getPlans().remove(worstPlan);
				carrier.getPlans().add(copiedPlan);
			}
		}
		
	}

	private CarrierPlan copyPlan(CarrierPlan selectedPlan) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for(ScheduledTour sTour : selectedPlan.getScheduledTours()){
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = new CarrierFactory().copyTour(sTour.getTour());
			tours.add(new CarrierFactory().createScheduledTour(tour, vehicle, depTime));
		}
		CarrierPlan plan = new CarrierPlan(tours);
		plan.setScore(selectedPlan.getScore());
		return plan;
	}

}
