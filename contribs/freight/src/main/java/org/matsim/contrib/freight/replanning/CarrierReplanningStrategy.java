package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.replanning.selectors.CarrierPlanSelector;

public class CarrierReplanningStrategy {

	private static Logger logger = Logger.getLogger(CarrierReplanningStrategy.class);

	private List<CarrierReplanningStrategyModule> strategyModules = new ArrayList<CarrierReplanningStrategyModule>();

	private CarrierPlanSelector carrierPlanSelector;
	
	public CarrierReplanningStrategy(CarrierPlanSelector carrierPlanSelector) {
		super();
		this.carrierPlanSelector = carrierPlanSelector;
	}

	public void addModule(CarrierReplanningStrategyModule module) {
		strategyModules.add(module);
	}

	public void run(Carrier carrier) {
		CarrierPlan plan = carrierPlanSelector.selectPlan(carrier);
		CarrierPlan copiedPlan = copyPlan(plan);
		
		for (CarrierReplanningStrategyModule module : strategyModules) {
			logger.info("run " + module.getClass().toString());
			module.handlePlan(copiedPlan);
		}
		
		addPlan(carrier,copiedPlan);
		carrier.setSelectedPlan(copiedPlan);
		
	}
	
	private void addPlan(Carrier carrier, CarrierPlan copiedPlan) {
		int memSize = Carrier.PLAN_MEMORY;
		if (carrier.getPlans().size() < memSize) {
			memorizePlan(carrier, copiedPlan);
		} else {
			removeWorstPlan(carrier);
			memorizePlan(carrier, copiedPlan);
		}

	}
	
	private void memorizePlan(Carrier carrier, CarrierPlan copiedPlan) {
		carrier.getPlans().add(copiedPlan);
	}


	private void removeWorstPlan(Carrier carrier) {
		CarrierPlan worstPlan = null;
		for (CarrierPlan p : carrier.getPlans()) {
			if (worstPlan == null) {
				worstPlan = p;
			} else {
				if (p.getScore() < worstPlan.getScore()) {
					worstPlan = p;
				}
			}
		}
		carrier.getPlans().remove(worstPlan);
	}


	private CarrierPlan copyPlan(CarrierPlan plan2copy) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : plan2copy.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(new CarrierFactory().createScheduledTour(tour, vehicle, depTime));
		}
		CarrierPlan copiedPlan = new CarrierPlan(plan2copy.getCarrier(), tours);
		copiedPlan.setScore(plan2copy.getScore());
		return copiedPlan;

	}

}
