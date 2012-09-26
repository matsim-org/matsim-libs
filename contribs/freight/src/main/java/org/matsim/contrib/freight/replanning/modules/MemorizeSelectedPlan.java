package org.matsim.contrib.freight.replanning.modules;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyModule;

public class MemorizeSelectedPlan implements CarrierPlanStrategyModule {

	private int planMemory = 2;
	
	public int getPlanMemory() {
		return planMemory;
	}

	public void setPlanMemory(int planMemory) {
		this.planMemory = planMemory;
	}

	@Override
	public void handleCarrier(Carrier carrier) {
		CarrierPlan copiedPlan = copyPlan(carrier.getSelectedPlan());
		addPlan(carrier, copiedPlan);
	}

	private void addPlan(Carrier carrier, CarrierPlan copiedPlan) {
		int memSize = planMemory;
		if (carrier.getPlans().size() < memSize) {
			memorizePlan(carrier, copiedPlan);
		} else {
			removeWorstAndMemorizePlan(carrier, copiedPlan);
		}

	}

	private void removeWorstAndMemorizePlan(Carrier carrier,CarrierPlan copiedPlan) {
		boolean toBeInserted = false;
		CarrierPlan worstPlan = null;
		for (CarrierPlan p : carrier.getPlans()) {
			if (copiedPlan.getScore() > p.getScore()) {
				toBeInserted = true;
			}
			if (worstPlan == null) {
				worstPlan = p;
			} else {
				if (p.getScore() < worstPlan.getScore()) {
					worstPlan = p;
				}
			}
		}
		if (toBeInserted) {
			carrier.getPlans().remove(worstPlan);
			memorizePlan(carrier, copiedPlan);
		}
	}

	private void memorizePlan(Carrier carrier, CarrierPlan copiedPlan) {
		carrier.getPlans().add(copiedPlan);
	}

	private CarrierPlan copyPlan(CarrierPlan selectedPlan) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : selectedPlan.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(new CarrierFactory().createScheduledTour(tour, vehicle,
					depTime));
		}
		CarrierPlan plan = new CarrierPlan(tours);
		plan.setScore(selectedPlan.getScore());
		return plan;
	}

}
