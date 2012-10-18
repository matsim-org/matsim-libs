package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;

public class SelectBestPlan implements CarrierPlanSelector {
	
//	private int planMemory = 5;
//	
//	public int getPlanMemory() {
//		return planMemory;
//	}
//
//	public void setPlanMemory(int planMemory) {
//		this.planMemory = planMemory;
//	}
//
//	@Override
//	public void handlePlan(CarrierPlan carrierPlan) {
//		CarrierPlan best = null;
//		for (CarrierPlan p : carrier.getPlans()) {
//			if (best == null) {
//				best = p;
//			} else {
//				if (p.getScore() > best.getScore()) {
//					best = p;
//				}
//			}
//		}
//		if (best != null) {
//			CarrierPlan copied = copyPlan(best);
//			addPlan(carrier, copied);
//			carrier.setSelectedPlan(copied);
//		}
//	}
//	
//	private CarrierPlan copyPlan(CarrierPlan selectedPlan) {
//		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
//		for (ScheduledTour sTour : selectedPlan.getScheduledTours()) {
//			double depTime = sTour.getDeparture();
//			CarrierVehicle vehicle = sTour.getVehicle();
//			Tour tour = sTour.getTour().duplicate();
//			tours.add(new CarrierFactory().createScheduledTour(tour, vehicle, depTime));
//		}
//		CarrierPlan plan = new CarrierPlan(carrier, tours);
//		plan.setScore(selectedPlan.getScore());
//		return plan;
//	}
//	
//	private void addPlan(Carrier carrier, CarrierPlan copiedPlan) {
//		int memSize = planMemory;
//		if (carrier.getPlans().size() < memSize) {
//			memorizePlan(carrier, copiedPlan);
//		} else {
//			removeWorstAndMemorizePlan(carrier, copiedPlan);
//		}
//
//	}
//	
//	private void memorizePlan(Carrier carrier, CarrierPlan copiedPlan) {
//		carrier.getPlans().add(copiedPlan);
//	}
//
//
//	private void removeWorstAndMemorizePlan(Carrier carrier,CarrierPlan copiedPlan) {
//		boolean toBeInserted = false;
//		CarrierPlan worstPlan = null;
//		for (CarrierPlan p : carrier.getPlans()) {
//			if (copiedPlan.getScore() > p.getScore()) {
//				toBeInserted = true;
//			}
//			if (worstPlan == null) {
//				worstPlan = p;
//			} else {
//				if (p.getScore() < worstPlan.getScore()) {
//					worstPlan = p;
//				}
//			}
//		}
//		if (toBeInserted) {
//			carrier.getPlans().remove(worstPlan);
//			memorizePlan(carrier, copiedPlan);
//		}
//	}

	@Override
	public CarrierPlan selectPlan(Carrier carrier) {
		CarrierPlan best = null;
		for (CarrierPlan p : carrier.getPlans()) {
			if(p.getScore() == null){
				return p;
			}
			if (best == null) {
				best = p;
			} else {
				if (p.getScore() > best.getScore()) {
					best = p;
				}
			}
		}
		return best;
	}


}
