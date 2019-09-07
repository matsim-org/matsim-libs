package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarrierUtils{
	public static Carrier createCarrier( Id<Carrier> id ){
		return new CarrierImpl(id);
	}

	public static CarrierPlan copyPlan( CarrierPlan plan2copy ) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : plan2copy.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(ScheduledTour.newInstance(tour, vehicle, depTime));
		}
		CarrierPlan copiedPlan = new CarrierPlan(plan2copy.getCarrier(), tours);
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		return copiedPlan;

	}

}
