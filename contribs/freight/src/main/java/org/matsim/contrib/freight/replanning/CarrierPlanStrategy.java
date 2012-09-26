package org.matsim.contrib.freight.replanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.ScheduledTour;

public class CarrierPlanStrategy {

	private Logger logger = Logger.getLogger(CarrierPlanStrategy.class);

	private List<CarrierPlanStrategyModule> strategyModules = new ArrayList<CarrierPlanStrategyModule>();

	public void addModule(CarrierPlanStrategyModule module) {
		strategyModules.add(module);
	}

	public void run(Carrier carrier) {
		for (CarrierPlanStrategyModule module : strategyModules) {
			logger.info("run " + module.getClass().toString());

			module.handleCarrier(carrier);
		}
		assertSelectedCarrierPlanIsConsistenWithContracts(carrier);
	}

	private void assertSelectedCarrierPlanIsConsistenWithContracts(
			Carrier carrier) {
		if (carrier.getSelectedPlan() == null) {
			return;
		}
		Set<CarrierShipment> contractedCarrierShipments = new HashSet<CarrierShipment>();
		for (CarrierShipment s : carrier.getShipments()) {
			contractedCarrierShipments.add(s);
		}
		Set<CarrierShipment> shipmentsInPlan = new HashSet<CarrierShipment>();
		for (ScheduledTour t : carrier.getSelectedPlan().getScheduledTours()) {
			shipmentsInPlan.addAll(t.getTour().getShipments());
		}
		for (CarrierShipment cF : contractedCarrierShipments) {
			if (!shipmentsInPlan.contains(cF)) {
				throw new IllegalStateException(
						"shipment in contracts not in plan");
			}
		}
		for (CarrierShipment cF : shipmentsInPlan) {
			if (!contractedCarrierShipments.contains(cF)) {
				throw new IllegalStateException(
						"shipment in plan not in contracts");
			}
		}

	}
}
